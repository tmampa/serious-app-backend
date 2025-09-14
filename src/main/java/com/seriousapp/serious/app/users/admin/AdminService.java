package com.seriousapp.serious.app.users.admin;

import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClientBuilder;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisOptions;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisResult;
import com.azure.ai.vision.imageanalysis.models.VisualFeatures;
import com.azure.core.credential.KeyCredential;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobAccessPolicy;
import com.azure.storage.blob.models.BlobSignedIdentifier;
import com.azure.storage.blob.models.PublicAccessType;
import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.book.BookService;
import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.borrowing.BorrowingRecordService;
import com.seriousapp.serious.app.configurations.EmailConfiguration;
import com.seriousapp.serious.app.contact.Email;
import com.seriousapp.serious.app.dto.BorrowRecordResponse;
import com.seriousapp.serious.app.dto.UserRequest;
import com.seriousapp.serious.app.users.student.Student;
import com.seriousapp.serious.app.users.student.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
@Slf4j
public class AdminService {
    private final AdminRepository adminRepository;
    private final BookService bookService;
    private final StudentService studentService;
    private final BorrowingRecordService borrowingRecordService;
    private final BlobServiceClient blobServiceClient;
    private final EmailConfiguration emailConfiguration;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Value("${computer.vision.endpoint}")
    private String computerVisionEndpoint;
    @Value("${computer.vision.key}")
    private String computerVisionKey;

    public AdminService(AdminRepository adminRepository,
                        BookService bookService,
                        StudentService studentService,
                        BorrowingRecordService borrowingRecordService,
                        BlobServiceClient blobServiceClient,
                        EmailConfiguration emailConfiguration, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.adminRepository = adminRepository;
        this.bookService = bookService;
        this.studentService = studentService;
        this.borrowingRecordService = borrowingRecordService;
        this.blobServiceClient = blobServiceClient;
        this.emailConfiguration = emailConfiguration;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public Admin findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    public Admin saveAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    public BorrowRecordResponse createBorrowRecord(UserRequest userRequest, String bookTitle) {
        Book book = bookService.getBookByName(bookTitle);
        if (book == null) {
            throw new RuntimeException("Book not found");
        }

        Student student = studentService.getStudent(userRequest);
        LocalDate borrowDate = LocalDate.now();

        BorrowingRecord record = new BorrowingRecord();
        record.setStudent(student);
        record.setBook(book);
        record.setBorrowDate(borrowDate);

        BorrowRecordResponse borrowRecordResponse = new BorrowRecordResponse();

        if (book.getStockQuantity() > 0) {
            book.setStockQuantity(book.getStockQuantity() - 1);
            studentService.saveStudent(student);
            bookService.saveBook(book);
            BorrowingRecord createdRecord = borrowingRecordService.save(record);

            borrowRecordResponse.setId(createdRecord.getId());
            borrowRecordResponse.setStudentId(student.getId());
            //borrowRecordResponse.setStudentName(student.getFullName());
            borrowRecordResponse.setStudentNumber(student.getStudentNumber().toString());
            borrowRecordResponse.setBookId(book.getId());
            borrowRecordResponse.setBookTitle(book.getTitle());
            borrowRecordResponse.setAuthor(book.getAuthor());
            borrowRecordResponse.setBorrowDate(createdRecord.getBorrowDate());

            return borrowRecordResponse;
        }
        throw new RuntimeException("Book is not available");
    }

    private String sanitizeContainerName(String name) {
        return name.toLowerCase()
                  .replaceAll("\\s+", "-")
                  .replaceAll("[^a-z0-9-]", "")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "")
                  .substring(0, Math.min(name.length(), 63));
    }

    public BorrowingRecord uploadBorrowImages(Long recordId, List<MultipartFile> images) {
        var record = borrowingRecordService.findById(recordId);
        if (record.isEmpty()) {
            throw new RuntimeException("Record not found");
        }

        BlobSignedIdentifier blobSignedIdentifier = new BlobSignedIdentifier()
                .setId("name")
                .setAccessPolicy(new BlobAccessPolicy());

        String currentMills = String.valueOf(System.currentTimeMillis());
        String bookTitle = record.get().getBook().getTitle();

        var containerName = sanitizeContainerName(bookTitle + "-" + currentMills);
        var blobContainerClient = blobServiceClient.createBlobContainerIfNotExists(containerName);
        blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER, List.of(blobSignedIdentifier));

        Set<String> computerVisionTags = new HashSet<>();
        Set<String> imagesURLS = new HashSet<>();

        ImageAnalysisClient client = new ImageAnalysisClientBuilder()
                .endpoint(computerVisionEndpoint)
                .credential(new KeyCredential(computerVisionKey))
                .buildClient();

        for (MultipartFile image : images) {
            processAndAnalyzeImage(image, blobContainerClient, client, computerVisionTags, imagesURLS);
        }

        record.get().setImages(imagesURLS);
        record.get().setTags(computerVisionTags);
        BorrowingRecord savedRecord = borrowingRecordService.save(record.get());

        sendBorrowEmail(savedRecord, imagesURLS);

        return savedRecord;
    }

    public double returnBook(Long studentNumber, String bookTitle, List<MultipartFile> images) {
        var bookBeingReturned = studentService.returnBook(studentNumber, bookTitle);

        String containerNameBase = String.format("%s-%s-%d-%s-%s",
            bookBeingReturned.getBook().getTitle(),
            //bookBeingReturned.getStudent().getFullName(),
            bookBeingReturned.getStudent().getStudentNumber(),
            LocalDate.now(),
            System.currentTimeMillis()
        );

        var containerName = sanitizeContainerName(containerNameBase);
        var blobContainerClient = blobServiceClient.createBlobContainerIfNotExists(containerName);
        blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER,
            List.of(new BlobSignedIdentifier().setId("name").setAccessPolicy(new BlobAccessPolicy())));

        Set<String> computerVisionTags = new HashSet<>();
        Set<String> imagesURLS = new HashSet<>();

        ImageAnalysisClient client = new ImageAnalysisClientBuilder()
                .endpoint(computerVisionEndpoint)
                .credential(new KeyCredential(computerVisionKey))
                .buildClient();

        for (MultipartFile image : images) {
            processAndAnalyzeImage(image, blobContainerClient, client, computerVisionTags, imagesURLS);
        }

        double amountOwed = calculateDamageFines(computerVisionTags, bookBeingReturned.getTags());

        bookBeingReturned.getStudent().setOutstandingFines(amountOwed);
        studentService.saveStudent(bookBeingReturned.getStudent());

        sendReturnEmail(bookBeingReturned, imagesURLS, amountOwed, computerVisionTags);

        return amountOwed;
    }

    private void processAndAnalyzeImage(MultipartFile image,
                                      BlobContainerClient blobContainerClient,
                                      ImageAnalysisClient client,
                                      Set<String> computerVisionTags,
                                      Set<String> imagesURLS) {
        String blobName = (image.getOriginalFilename() != null ? image.getOriginalFilename() : "unnamed-" + System.currentTimeMillis())
                .replaceAll("\\s+", "-")
                .toLowerCase();
        var imageBlob = blobContainerClient.getBlobClient(blobName);

        try {
            imageBlob.deleteIfExists();
            imageBlob.upload(image.getInputStream());
            String imageUrl = imageBlob.getBlobUrl();
            imagesURLS.add(imageUrl);

            ImageAnalysisResult result = client.analyzeFromUrl(
                    imageUrl,
                    Collections.singletonList(VisualFeatures.TAGS),
                    new ImageAnalysisOptions().setGenderNeutralCaption(true));

            if (result.getTags() != null) {
                result.getTags().getValues().forEach(tag -> computerVisionTags.add(tag.getName()));
            }
        } catch (IOException e) {
            log.error("Error uploading image to blob storage: {}", e.getMessage(), e);
        }
    }

    private double calculateDamageFines(Set<String> newTags, Set<String> originalTags) {
        Map<String, Double> tagPrices = getDamageTagPrices();
        double defaultPrice = 9.99;
        double amountOwed = 0.0;

        Set<String> damagesTags = new HashSet<>(newTags);
        for (String tag : originalTags) {
            damagesTags.removeIf(a -> a.equalsIgnoreCase(tag));
        }

        for (String tag : damagesTags) {
            amountOwed += tagPrices.getOrDefault(tag.toLowerCase(), defaultPrice);
        }

        return amountOwed;
    }

    private Map<String, Double> getDamageTagPrices() {
        Map<String, Double> tagPrices = new HashMap<>();
        tagPrices.put("missing book cover", 90.0);
        tagPrices.put("coffee stain", 20.0);
        tagPrices.put("torn pages", 50.0);
        // ...existing damage prices...
        return tagPrices;
    }

    private void sendBorrowEmail(BorrowingRecord record, Set<String> imagesURLS) {
        Student student = record.getStudent();
        Book book = record.getBook();
        List<String> parentEmails = student.getEmails().stream()
                .map(Email::getEmail)
                .toList();

        if (parentEmails.isEmpty()) {
            log.warn("No parent emails found for student: {}", student);
            return;
        }

        String subject = String.format("%s has borrowed %s", student.getStudentNumber(), book.getTitle());
        String plainText = createBorrowEmailPlainText(student, book, record, imagesURLS);
        String htmlText = createBorrowEmailHtml(student, book, record, imagesURLS);

        emailConfiguration.sendEmail(parentEmails, subject, plainText, htmlText);
    }

    private void sendReturnEmail(BorrowingRecord record, Set<String> imagesURLS, double amountOwed, Set<String> damages) {
        Student student = record.getStudent();
        Book book = record.getBook();
        List<String> parentEmails = student.getEmails().stream()
                .map(Email::getEmail)
                .toList();

        if (parentEmails.isEmpty()) {
            log.warn("No parent emails found for student: {}", student.getStudentNumber());
            return;
        }

        String subject = String.format("%s has returned %s", student.getStudentNumber(), book.getTitle());
        String plainText = createReturnEmailPlainText(student, book, imagesURLS, amountOwed, damages);
        String htmlText = createReturnEmailHtml(student, book, imagesURLS, amountOwed, damages);

        emailConfiguration.sendEmail(parentEmails, subject, plainText, htmlText);
    }

    // Helper methods for email content creation
    private String createBorrowEmailPlainText(Student student, Book book, BorrowingRecord record, Set<String> imagesURLS) {
        StringBuilder body = new StringBuilder();
        body.append("Dear Parent/Guardian,\n\n");
        // ...rest of the plain text email content...
        return body.toString();
    }

    private String createBorrowEmailHtml(Student student, Book book, BorrowingRecord record, Set<String> imagesURLS) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Dear Parent/Guardian,</p>");
        // ...rest of the HTML email content...
        return html.toString();
    }

    private String createReturnEmailPlainText(Student student, Book book, Set<String> imagesURLS, double amountOwed, Set<String> damages) {
        StringBuilder body = new StringBuilder();
        body.append("Dear Parent/Guardian,\n\n");
        // ...rest of the plain text email content...
        return body.toString();
    }

    private String createReturnEmailHtml(Student student, Book book, Set<String> imagesURLS, double amountOwed, Set<String> damages) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Dear Parent/Guardian,</p>");
        // ...rest of the HTML email content...
        return html.toString();
    }

    public Admin createAdmin(Admin admin) {
        String rawPassword = "myPassword123";
        String encoded = this.bCryptPasswordEncoder.encode(rawPassword);
        boolean matches = this.bCryptPasswordEncoder.matches(rawPassword, encoded);
        log.info("Password matches: {}", matches);

        String encodedPassword = this.bCryptPasswordEncoder.encode(admin.getPassword());
        admin.setPassword(encodedPassword);

        log.info("admin obj before saving: {}", admin);

        return adminRepository.save(
                new Admin(
                        admin.getUsername(),
                        admin.getEmail(),
                        admin.getPassword(),
                        admin.getEmployeeId()
                )
        );
    }
}
