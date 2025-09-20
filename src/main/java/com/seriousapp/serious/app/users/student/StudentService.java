package com.seriousapp.serious.app.users.student;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.borrowing.BorrowingRecordService;
import com.seriousapp.serious.app.parent.ParentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.book.BookService;
import com.seriousapp.serious.app.dto.UserRequest;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClientBuilder;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisOptions;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisResult;
import com.azure.ai.vision.imageanalysis.models.VisualFeatures;
import com.azure.core.credential.KeyCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobAccessPolicy;
import com.azure.storage.blob.models.BlobSignedIdentifier;
import com.azure.storage.blob.models.PublicAccessType;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final BookService bookService;
    private final BorrowingRecordService borrowingRecordService;
    @Value("${computer.vision.endpoint}")
    private String computerVisionEndpoint;
    @Value("${computer.vision.key}")
    private String computerVisionKey;
    private final ParentService parentService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final BlobServiceClient blobServiceClient;

    public StudentService(
            StudentRepository studentRepository,
            BookService bookService,
            BorrowingRecordService borrowingRecordService,
            ParentService parentService,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            BlobServiceClient blobServiceClient
    ) {
        this.studentRepository = studentRepository;
        this.bookService = bookService;
        this.borrowingRecordService = borrowingRecordService;
        this.parentService = parentService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.blobServiceClient = blobServiceClient;
    }

    @Transactional
    public void borrowBook(UserRequest userRequest, String bookName, List<MultipartFile> images) throws IOException, IllegalAccessException {
        Book book = bookService.getBookByName(bookName);

        if (book == null) {
            throw new RuntimeException("Book not found");
        }

        Student student = this.studentRepository.findByFullName(userRequest.getFullName())
                .or(() -> this.studentRepository.findByStudentNumber(userRequest.getStudentNumber()))
                .orElse(new Student());

        //student.setFullName(userRequest.getFullName());
        student.setStudentNumber(userRequest.getStudentNumber());
        // Use safe collection replacement methods
        //student.re(userRequest.getEmails());
        //student.replacePhoneNumbers(userRequest.getPhoneNumbers());
        student.setAddress(userRequest.getAddress());
        student.setOutstandingFines(0);

        BlobSignedIdentifier blobSignedIdentifier = new BlobSignedIdentifier()
                .setId("name")
                .setAccessPolicy(new BlobAccessPolicy());

        var originalContainerName = bookName.replaceAll("\\s+", "-").toLowerCase();
        var blobContainerClient = blobServiceClient.createBlobContainerIfNotExists(originalContainerName);
        blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER, List.of(blobSignedIdentifier));

        Set<String> computerVisionTags = new java.util.HashSet<>();
        if (computerVisionEndpoint == null || computerVisionKey == null) {
            log.error("Missing environment variable 'VISION_ENDPOINT' or 'VISION_KEY'.");
            log.error("Set them before running this sample.");
        }

        // Create a synchronous client using API key authentication
        ImageAnalysisClient client = new ImageAnalysisClientBuilder()
                .endpoint(computerVisionEndpoint)
                .credential(new KeyCredential(computerVisionKey))
                .buildClient();

        for (MultipartFile image : images) {
            var imageBlob = blobContainerClient.getBlobClient(image.getOriginalFilename()
                    .replaceAll("\\s+", "-").toLowerCase());

            try {
                imageBlob.upload(image.getInputStream());
                String imageUrl = imageBlob.getBlobUrl();

                log.info("Image uploaded to blob storage: {}", imageUrl);

                ImageAnalysisResult result = client.analyzeFromUrl(
                        imageUrl,
                        Collections.singletonList(VisualFeatures.TAGS),
                        new ImageAnalysisOptions().setGenderNeutralCaption(true));

                log.info("Image tags: {}", result.getTags());

                computerVisionTags.add(result.getTags().toString());

            } catch (IOException e) {
                log.error("Error uploading image to blob storage: {}", e.getMessage(), e);
            }
        }

        book.setImagesURL(blobContainerClient.getBlobContainerUrl());
        book.setImages(images.stream().map(MultipartFile::getOriginalFilename).toList());
        book.setTags(computerVisionTags);

        // Create borrowing record
        LocalDate borrowDate = LocalDate.now();

        BorrowingRecord record = new BorrowingRecord();
        record.setStudent(student);
        record.setBook(book);
        record.setBorrowDate(borrowDate);

        if (book.getStockQuantity() > 0) {
            book.setStockQuantity(book.getStockQuantity() - 1);
            this.studentRepository.save(student);
            this.bookService.saveBook(book);
            this.borrowingRecordService.save(record);
        }
    }

    public BorrowingRecord returnBook(Long studentNumber, String bookName) {
        Book book = bookService.getBookByName(bookName);

        Student student = this.studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(
                        () -> new RuntimeException(
                                "student " + studentNumber + " number of {} not found!"
                        )
                );

        // Find active borrowing record
        BorrowingRecord record = this.borrowingRecordService
                .findByStudentAndBookAndReturnDateIsNull(student, book)
                .orElseThrow(() -> new RuntimeException("record has been closed, no active borrowing found because book has been returned"));


        // Update return date
        record.setReturnDate(LocalDate.now());

        // Update book quantity
        book.setStockQuantity(book.getStockQuantity() + 1);
        this.bookService.saveBook(book);

        // Save updated record
        return this.borrowingRecordService.save(record);
    }

    public List<Student> getAllUsers() {
        return studentRepository.findAll();
    }

    public List<Book> getBorrowedBooksByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<BorrowingRecord> records = this.borrowingRecordService
                .findByStudentAndReturnDateIsNull(student);

        return records.stream()
                .map(BorrowingRecord::getBook)
                .collect(Collectors.toList());
    }

    public Student saveStudent(Student student) {
        return this.studentRepository.save(student);
    }

    public Student findByEmail(String email) {
        return this.studentRepository.findByEmail(email);
    }

    public Student findById(Long studentId) {
        return this.studentRepository.findById(studentId).orElse(null);
    }

    @Transactional
    public void clearFines(Long studentId) {
        Student student = this.studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        student.setOutstandingFines(0);
        this.studentRepository.save(student);
    }

    public Student createStudent(Student student) {
        String encodedPassword = bCryptPasswordEncoder.encode(student.getPassword());
        student.setPassword(encodedPassword);
        return studentRepository.save(
                new Student(
                        student.getUsername(),
                        student.getEmail(),
                        student.getPassword(),
                        student.getFirstNames(),
                        student.getLastName(),
                        student.getAddress(),
                        student.getOutstandingFines(),
                        student.getStudentNumber()
                )
        );
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }

    public List<Student> getAllStudents() {
        return this.studentRepository.findAll();
    }

    public Optional<Student> getStudentByStudentNumber(Long studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber);
    }
}
