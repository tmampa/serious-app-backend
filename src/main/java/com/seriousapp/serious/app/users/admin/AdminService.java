package com.seriousapp.serious.app.users.admin;

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
import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.book.BookService;
import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.borrowing.BorrowingRecordService;
import com.seriousapp.serious.app.configurations.EmailConfiguration;
import com.seriousapp.serious.app.parent.Parent;
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
import java.util.stream.Collectors;

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

        Optional<Student> student = studentService.getStudentByStudentNumber(userRequest.getStudentNumber());

        if (student.isEmpty()) {
            throw new RuntimeException("Student not found");
        }

        LocalDate borrowDate = LocalDate.now();

        BorrowingRecord record = new BorrowingRecord();
        record.setStudent(student.get());
        record.setBook(book);
        record.setBorrowDate(borrowDate);
        record.setDueDate(userRequest.getDueDate());
        record.setBarcode(userRequest.getBarcode());

        BorrowRecordResponse borrowRecordResponse = new BorrowRecordResponse();

        studentService.saveStudent(student.get());
        bookService.saveBook(book);
        BorrowingRecord createdRecord = borrowingRecordService.save(record);

        borrowRecordResponse.setId(createdRecord.getId());
        borrowRecordResponse.setStudentId(student.get().getId());
        borrowRecordResponse.setStudentName(student.get().getFirstNames() + student.get().getLastName());
        borrowRecordResponse.setStudentNumber(student.get().getStudentNumber().toString());
        borrowRecordResponse.setBookId(book.getId());
        borrowRecordResponse.setBookTitle(book.getTitle());
        borrowRecordResponse.setAuthor(book.getAuthor());
        borrowRecordResponse.setBorrowDate(createdRecord.getBorrowDate());

        return borrowRecordResponse;
    }

    private String sanitizeContainerName(String name) {
        return name.toLowerCase()
                  .replaceAll("\\s+", "-")
                  .replaceAll("[^a-z0-9-]", "")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "")
                  .substring(0, Math.min(name.length(), 63));
    }

    public BorrowingRecord uploadBorrowImages(Long recordId, List<MultipartFile> images, List<String> knownTags) {
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

        if (knownTags != null) {
            computerVisionTags.addAll(knownTags);
        }

        record.get().setImages(imagesURLS);
        record.get().setTags(computerVisionTags);
        BorrowingRecord savedRecord = borrowingRecordService.save(record.get());

        sendBorrowEmail(savedRecord, imagesURLS);

        return savedRecord;
    }

    public double returnBook(Long studentNumber, String bookTitle, List<MultipartFile> images, List<String> knownTags) {
        var bookBeingReturned = studentService.returnBook(studentNumber, bookTitle);

        String containerNameBase = String.format("%s-%s-%d-%s-%s",
            bookBeingReturned.getBook().getTitle(),
            bookBeingReturned.getStudent().getFirstNames() + bookBeingReturned.getStudent().getLastName(),
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

        if (knownTags != null) {
            computerVisionTags.addAll(knownTags);
        }

        var savedTags = bookBeingReturned.getTags().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        var computerVisionAndKnowTagsInLowercase = computerVisionTags.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        computerVisionAndKnowTagsInLowercase.removeIf(savedTags::contains);

        double amountOwed;
        if (computerVisionAndKnowTagsInLowercase.contains("lost")) {
            amountOwed = bookBeingReturned.getBook().getPrice();
        } else {
            amountOwed = calculateDamageFines(computerVisionAndKnowTagsInLowercase);
        }

        double existingFines = bookBeingReturned.getStudent().getOutstandingFines();
        bookBeingReturned.getStudent().setOutstandingFines(existingFines + amountOwed);
        studentService.saveStudent(bookBeingReturned.getStudent());

        sendReturnEmail(bookBeingReturned, imagesURLS, amountOwed, computerVisionTags);

        return amountOwed;
    }

    private double calculateDamageFines(Set<String> newTags) {
        Map<String, Double> tagPrices = getDamageTagPrices();
        double defaultPrice = 9.99;
        double amountOwed = 0.0;

        for (String tag : newTags) {
            amountOwed += tagPrices.getOrDefault(tag.toLowerCase(), defaultPrice);
        }

        return amountOwed;
    }

    private Map<String, Double> getDamageTagPrices() {
        Map<String, Double> tagPrices = new HashMap<>();
        tagPrices.put("missing book cover", 90.0);
        tagPrices.put("coffee stain", 20.0);
        tagPrices.put("torn pages", 50.0);
        tagPrices.put("water damage", 40.0);
        tagPrices.put("writing", 15.0);
        tagPrices.put("highlighting", 10.0);
        tagPrices.put("bent cover", 25.0);
        tagPrices.put("bent pages", 15.0);
        tagPrices.put("sticker", 5.0);
        tagPrices.put("scratched cover", 30.0);
        tagPrices.put("dog ear", 10.0);
        tagPrices.put("faded cover", 20.0);
        tagPrices.put("loose pages", 35.0);
        tagPrices.put("mold", 60.0);
        tagPrices.put("tape", 10.0);
        tagPrices.put("missing pages", 80.0);
        tagPrices.put("burnt pages", 100.0);
        tagPrices.put("bent spine", 30.0);
        tagPrices.put("broken spine", 70.0);
        tagPrices.put("stained cover", 25.0);
        tagPrices.put("dirty cover", 15.0);
        tagPrices.put("scratched pages", 20.0);
        tagPrices.put("ripped cover", 50.0);
        tagPrices.put("ripped pages", 40.0);
        tagPrices.put("folded pages", 10.0);
        tagPrices.put("cracked cover", 45.0);
        tagPrices.put("cracked pages", 30.0);
        tagPrices.put("peeling cover", 35.0);
        tagPrices.put("peeling pages", 20.0);
        tagPrices.put("discoloration", 25.0);
        tagPrices.put("faded pages", 15.0);
        tagPrices.put("creased cover", 20.0);
        tagPrices.put("creased pages", 10.0);
        tagPrices.put("frayed edges", 15.0);
        tagPrices.put("loose binding", 40.0);
        tagPrices.put("broken binding", 80.0);
        tagPrices.put("missing dust jacket", 30.0);
        tagPrices.put("torn dust jacket", 20.0);
        tagPrices.put("damaged dust jacket", 25.0);
        tagPrices.put("water stain", 30.0);
        tagPrices.put("ink stain", 20.0);
        tagPrices.put("food stain", 25.0);
        tagPrices.put("grease stain", 20.0);
        tagPrices.put("paint stain", 30.0);
        tagPrices.put("glue stain", 15.0);
        tagPrices.put("torn cover page", 50.0);
        tagPrices.put("torn back cover", 50.0);
        tagPrices.put("damaged cover page", 30.0);
        tagPrices.put("damaged back cover", 30.0);
        tagPrices.put("loose cover", 40.0);
        tagPrices.put("loose back cover", 40.0);
        tagPrices.put("broken cover", 70.0);
        tagPrices.put("broken back cover", 70.0);
        tagPrices.put("missing cover page", 90.0);
        tagPrices.put("missing back cover", 90.0);
        return tagPrices;
    }

    private void sendBorrowEmail(BorrowingRecord record, Set<String> imagesURLS) {
        Student student = record.getStudent();
        Book book = record.getBook();
        List<String> parentEmails = new ArrayList<>(student.getParents().stream()
                .map(Parent::getEmail)
                .toList());

        String subject = String.format("%s has borrowed %s", student.getStudentNumber(), book.getTitle());
        String plainText = createBorrowEmailPlainText(student, book, record, imagesURLS);
        String htmlText = createBorrowEmailHtml(student, book, record, imagesURLS);

        emailConfiguration.sendEmail(parentEmails, subject, plainText, htmlText);
    }

    private void sendReturnEmail(BorrowingRecord record, Set<String> imagesURLS, double amountOwed, Set<String> damages) {
        Student student = record.getStudent();
        Book book = record.getBook();
        List<String> parentEmails = student.getParents().stream()
                .map(Parent::getEmail)
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
        body.append("We are pleased to inform you that your child has borrowed a book from the school library.\n\n");
        body.append("Student Details:\n");
        body.append("  Name: ").append(student.getFirstNames()).append(" ").append(student.getLastName()).append("\n");
        body.append("  Student Number: ").append(student.getStudentNumber()).append("\n\n");
        body.append("Book Details:\n");
        body.append("  Title: ").append(book.getTitle()).append("\n");
        body.append("  Author: ").append(book.getAuthor()).append("\n");
        body.append("  Borrow Date: ").append(record.getBorrowDate()).append("\n\n");
        body.append("Please ensure the book is returned by the due date in good condition to avoid any fines.\n");
        body.append("If you would like to see the condition of the book at the time of borrowing, you can view the following images:\n");
        for (String url : imagesURLS) {
            body.append("  ").append(url).append("\n");
        }
        body.append("\nIf you have any questions, please contact the school library.\n\n");
        body.append("Best regards,\n");
        body.append("School Library Team");
        return body.toString();
    }

    private String createBorrowEmailHtml(Student student, Book book, BorrowingRecord record, Set<String> imagesURLS) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">");

        html.append("<p>Dear Parent/Guardian,</p>");
        html.append("<p>We are pleased to inform you that your child has borrowed a book from the school library.</p>");

        html.append("<div style=\"margin: 20px 0;\">");
        html.append("<h3 style=\"color: #2c5282;\">Student Details:</h3>");
        html.append("<ul style=\"list-style-type: none; padding-left: 20px;\">");
        html.append("<li><strong>Name:</strong> ").append(student.getFirstNames()).append(" ").append(student.getLastName()).append("</li>");
        html.append("<li><strong>Student Number:</strong> ").append(student.getStudentNumber()).append("</li>");
        html.append("</ul>");
        html.append("</div>");

        html.append("<div style=\"margin: 20px 0;\">");
        html.append("<h3 style=\"color: #2c5282;\">Book Details:</h3>");
        html.append("<ul style=\"list-style-type: none; padding-left: 20px;\">");
        html.append("<li><strong>Title:</strong> ").append(book.getTitle()).append("</li>");
        html.append("<li><strong>Author:</strong> ").append(book.getAuthor()).append("</li>");
        html.append("<li><strong>Borrow Date:</strong> ").append(record.getBorrowDate()).append("</li>");
        html.append("</ul>");
        html.append("</div>");

        html.append("<p style=\"color: #744210; background-color: #fefcbf; padding: 15px; border-radius: 4px;\">");
        html.append("Please ensure the book is returned by the due date in good condition to avoid any fines.");
        html.append("</p>");

        if (!imagesURLS.isEmpty()) {
            html.append("<div style=\"margin: 20px 0;\">");
            html.append("<p>If you would like to see the condition of the book at the time of borrowing, you can view the following images:</p>");
            html.append("<div style=\"display: flex; flex-wrap: wrap; gap: 10px;\">");
            for (String url : imagesURLS) {
                html.append("<div>");
                html.append("<img src=\"").append(url).append("\" alt=\"Book condition\" ");
                html.append("style=\"max-width: 300px; border: 1px solid #e2e8f0; border-radius: 4px; margin: 5px;\"/>");
                html.append("</div>");
            }
            html.append("</div>");
            html.append("</div>");
        }

        html.append("<p style=\"margin-top: 20px;\">If you have any questions, please contact the school library.</p>");

        html.append("<p style=\"margin-top: 30px;\">");
        html.append("Best regards,<br>");
        html.append("School Library Team");
        html.append("</p>");

        html.append("</body></html>");
        return html.toString();
    }

    private String createReturnEmailPlainText(Student student, Book book, Set<String> imagesURLS, double amountOwed, Set<String> damages) {
        StringBuilder body = new StringBuilder();
        body.append("Dear Parent/Guardian,\n\n");
        body.append("We have received the returned book from your child.\n\n");
        body.append("Student Details:\n");
        body.append("  Name: ").append(student.getFirstNames()).append(" ").append(student.getLastName()).append("\n");
        body.append("  Student Number: ").append(student.getStudentNumber()).append("\n\n");
        body.append("Book Details:\n");
        body.append("  Title: ").append(book.getTitle()).append("\n");
        body.append("  Author: ").append(book.getAuthor()).append("\n");
        body.append("  Return Date: ").append(LocalDate.now()).append("\n\n");

        if (amountOwed > 0) {
            body.append("Damage Details:\n");
            for (String tag : damages) {
                body.append("  - ").append(tag).append("\n");
            }
            body.append("\n");
            body.append("Total Amount Owed: R").append(String.format("%.2f", amountOwed)).append("\n");
        } else {
            body.append("The book has been returned in good condition. No fines are due.\n");
        }

        body.append("\nIf you would like to see the condition of the book upon return, you can view the following images:\n");
        for (String url : imagesURLS) {
            body.append("  ").append(url).append("\n");
        }
        body.append("\nIf you have any questions, please contact the school library.\n\n");
        body.append("Best regards,\n");
        body.append("School Library Team");
        return body.toString();
    }

    private String createReturnEmailHtml(Student student, Book book, Set<String> imagesURLS, double amountOwed, Set<String> damages) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">");

        html.append("<p>Dear Parent/Guardian,</p>");
        html.append("<p>We have received the returned book from your child.</p>");

        html.append("<div style=\"margin: 20px 0;\">");
        html.append("<h3 style=\"color: #2c5282;\">Student Details:</h3>");
        html.append("<ul style=\"list-style-type: none; padding-left: 20px;\">");
        html.append("<li><strong>Name:</strong> ").append(student.getFirstNames()).append(" ").append(student.getLastName()).append("</li>");
        html.append("<li><strong>Student Number:</strong> ").append(student.getStudentNumber()).append("</li>");
        html.append("</ul>");
        html.append("</div>");

        html.append("<div style=\"margin: 20px 0;\">");
        html.append("<h3 style=\"color: #2c5282;\">Book Details:</h3>");
        html.append("<ul style=\"list-style-type: none; padding-left: 20px;\">");
        html.append("<li><strong>Title:</strong> ").append(book.getTitle()).append("</li>");
        html.append("<li><strong>Author:</strong> ").append(book.getAuthor()).append("</li>");
        html.append("<li><strong>Return Date:</strong> ").append(LocalDate.now()).append("</li>");
        html.append("</ul>");
        html.append("</div>");

        if (amountOwed > 0) {
            html.append("<div style=\"color: #744210; background-color: #fefcbf; padding: 15px; border-radius: 4px; margin: 20px 0;\">");
            html.append("<h4 style=\"margin: 0;\">Damage Details:</h4>");
            html.append("<ul style=\"list-style-type: none; padding-left: 20px; margin: 10px 0;\">");
            for (String tag : damages) {
                html.append("<li>").append(tag).append("</li>");
            }
            html.append("</ul>");
            html.append("<p style=\"margin: 0;\">Total Amount Owed: <strong>R").append(String.format("%.2f", amountOwed)).append("</strong></p>");
            html.append("</div>");
        } else {
            html.append("<p style=\"color: #2f855a;\">The book has been returned in good condition. No fines are due.</p>");
        }

        if (!imagesURLS.isEmpty()) {
            html.append("<div style=\"margin: 20px 0;\">");
            html.append("<p>If you would like to see the condition of the book upon return, you can view the following images:</p>");
            html.append("<div style=\"display: flex; flex-wrap: wrap; gap: 10px;\">");
            for (String url : imagesURLS) {
                html.append("<div>");
                html.append("<img src=\"").append(url).append("\" alt=\"Book condition\" ");
                html.append("style=\"max-width: 300px; border: 1px solid #e2e8f0; border-radius: 4px; margin: 5px;\"/>");
                html.append("</div>");
            }
            html.append("</div>");
            html.append("</div>");
        }

        html.append("<p style=\"margin-top: 20px;\">If you have any questions, please contact the school library.</p>");

        html.append("<p style=\"margin-top: 30px;\">");
        html.append("Best regards,<br>");
        html.append("School Library Team");
        html.append("</p>");

        html.append("</body></html>");
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
