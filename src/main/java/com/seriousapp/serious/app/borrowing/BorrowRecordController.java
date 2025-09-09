package com.seriousapp.serious.app.borrowing;

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
import com.seriousapp.serious.app.configurations.EmailConfiguration;
import com.seriousapp.serious.app.contact.Email;
import com.seriousapp.serious.app.dto.BorrowRecordResponse;
import com.seriousapp.serious.app.dto.UserRequest;
import com.seriousapp.serious.app.users.Student;
import com.seriousapp.serious.app.users.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/borrow")
@Slf4j
public class BorrowRecordController {
    private final BookService bookService;
    private final StudentService studentService;
    private final BorrowingRecordService borrowingRecordService;
    private final BlobServiceClient blobServiceClient;
    private final EmailConfiguration emailConfiguration;
    @Value("${computer.vision.endpoint}")
    private String computerVisionEndpoint;
    @Value("${computer.vision.key}")
    private String computerVisionKey;

    public BorrowRecordController(BookService bookService, StudentService studentService, BorrowingRecordService borrowingRecordService, BlobServiceClient blobServiceClient, EmailConfiguration emailConfiguration) {
        this.bookService = bookService;
        this.studentService = studentService;
        this.borrowingRecordService = borrowingRecordService;
        this.blobServiceClient = blobServiceClient;
        this.emailConfiguration = emailConfiguration;
    }


    @PostMapping("/create-record/{book_title}")
    public ResponseEntity<?> createBorrowRecord(
            @RequestBody UserRequest userRequest,
            @PathVariable("book_title") String bookTitle
    ) {
        Book book = bookService.getBookByName(bookTitle);

        if (book == null) {
            throw new RuntimeException("Book not found");
        }

        Student student = this.studentService.getStudent(userRequest);

        // Create borrowing record
        LocalDate borrowDate = LocalDate.now();

        BorrowingRecord record = new BorrowingRecord();
        record.setStudent(student);
        record.setBook(book);
        record.setBorrowDate(borrowDate);

        BorrowRecordResponse borrowRecordResponse = new BorrowRecordResponse();

        if (book.getStockQuantity() > 0) {
            book.setStockQuantity(book.getStockQuantity() - 1);
            this.studentService.saveStudent(student);
            this.bookService.saveBook(book);
            BorrowingRecord createdRecord = this.borrowingRecordService.save(record);

            // Map to simplified response
            borrowRecordResponse.setId(createdRecord.getId());
            borrowRecordResponse.setStudentId(student.getId());
            borrowRecordResponse.setStudentName(student.getFullName());
            borrowRecordResponse.setStudentNumber(student.getStudentNumber().toString());
            borrowRecordResponse.setBookId(book.getId());
            borrowRecordResponse.setBookTitle(book.getTitle());
            borrowRecordResponse.setAuthor(book.getAuthor());
            borrowRecordResponse.setBorrowDate(createdRecord.getBorrowDate());

            return new ResponseEntity<>(borrowRecordResponse, HttpStatus.CREATED);
        }
        return new ResponseEntity<>(borrowRecordResponse, HttpStatus.BAD_REQUEST);
    }

    private String sanitizeContainerName(String name) {
        return name.toLowerCase()
                  .replaceAll("\\s+", "-")           // Replace spaces with hyphens
                  .replaceAll("[^a-z0-9-]", "")      // Remove any character that's not a lowercase letter, number, or hyphen
                  .replaceAll("-+", "-")             // Replace multiple consecutive hyphens with a single one
                  .replaceAll("^-|-$", "")           // Remove leading/trailing hyphens
                  .substring(0, Math.min(name.length(), 63)); // Azure container names must be 3-63 chars
    }

    @PutMapping("/upload-images/{recordId}")
    public ResponseEntity<?> uploadImages(
            @PathVariable("recordId") Long recordId,
            @RequestParam("images")List<MultipartFile> images
    ) {

        var record = this.borrowingRecordService.findById(recordId);

        if (record.isEmpty()) {
            return new ResponseEntity<>("record not found", HttpStatus.NOT_FOUND);
        }

        BlobSignedIdentifier blobSignedIdentifier = new BlobSignedIdentifier()
                .setId("name")
                .setAccessPolicy(new BlobAccessPolicy());

        String currentMills = String.valueOf(System.currentTimeMillis());
        String bookTitle = record.get().getBook().getTitle();

        var originalContainerName = sanitizeContainerName(bookTitle + "-" + currentMills);
        var blobContainerClient = blobServiceClient.createBlobContainerIfNotExists(originalContainerName);
        blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER, List.of(blobSignedIdentifier));

        Set<String> computerVisionTags = new HashSet<>();
        Set<String> imagesURLS = new HashSet<>();

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

                // Extract just the tag names as strings
                if (result.getTags() != null) {
                    for (var tag : result.getTags().getValues()) {
                        computerVisionTags.add(tag.getName());
                        log.info("Image tag name: {}", tag);
                    }
                }
            } catch (Exception e) {
                log.error("Error uploading image to blob storage: {}", e.getMessage(), e);
            }
        }

        record.get().setImages(imagesURLS);
        record.get().setTags(computerVisionTags);
        this.borrowingRecordService.save(record.get());


        // Email logic moved outside the image upload loop
        Student student = record.get().getStudent();
        Book book = record.get().getBook();
        List<String> parentEmails = student.getEmails().stream()
                .map(Email::getEmail)
                .toList();

        String subject = student.getFullName() + " has borrowed " + book.getTitle();
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Dear Parent/Guardian,\n\n");
        bodyBuilder.append("We wish to inform you that your child, ")
                .append(student.getFullName())
                .append(" (Student Number: ")
                .append(student.getStudentNumber())
                .append(") has borrowed the following book:\n\n");
        bodyBuilder.append("Book Title: ").append(book.getTitle()).append("\n");
        bodyBuilder.append("Author: ").append(book.getAuthor() != null ? book.getAuthor() : "N/A").append("\n");
        bodyBuilder.append("Borrow Date: ").append(record.get().getBorrowDate()).append("\n\n");
        bodyBuilder.append("Images showing the state of the book at borrowing:\n");
        for (String url : imagesURLS) {
            bodyBuilder.append(url).append("\n");
        }
        bodyBuilder.append("\nPlease keep this for your records.\n\nRegards,\nSeriousApp Team");

        // HTML body
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<p>Dear Parent/Guardian,</p>");
        htmlBuilder.append("<p>We wish to inform you that your child, <strong>")
                .append(student.getFullName())
                .append("</strong> (Student Number: ")
                .append(student.getStudentNumber())
                .append(") has borrowed the following book:</p>");
        htmlBuilder.append("<ul>");
        htmlBuilder.append("<li><strong>Book Title:</strong> ").append(book.getTitle()).append("</li>");
        htmlBuilder.append("<li><strong>Author:</strong> ").append(book.getAuthor() != null ? book.getAuthor() : "N/A").append("</li>");
        htmlBuilder.append("<li><strong>Borrow Date:</strong> ").append(record.get().getBorrowDate()).append("</li>");
        htmlBuilder.append("</ul>");
        htmlBuilder.append("<p>Images showing the state of the book at borrowing:</p><ul>");
        for (String url : imagesURLS) {
            htmlBuilder.append("<li><a href='")
                    .append(url)
                    .append("' target='_blank'>View Image</a></li>");
        }
        htmlBuilder.append("</ul>");
        htmlBuilder.append("<p>Please keep this for your records.<br><br>Regards,<br>SeriousApp Team</p>");

        if (!parentEmails.isEmpty()) {
            emailConfiguration.sendEmail(parentEmails, subject, bodyBuilder.toString(), htmlBuilder.toString());
        } else {
            log.warn("No parent emails found for student: {}", student.getFullName());
        }

        return new ResponseEntity<>(record.get(), HttpStatus.OK);
    }

    @PutMapping("/return-book/{studentNumber}/{bookTitle}")
    public ResponseEntity<?> returnBorrowedBook(
            @PathVariable("studentNumber") Long studentNumber,
            @PathVariable("bookTitle") String bookTitle,
            @RequestParam("images") List<MultipartFile> images
    ) {

        var bookBeingReturned = this.studentService.returnBook(studentNumber, bookTitle);

        BlobSignedIdentifier blobSignedIdentifier = new BlobSignedIdentifier()
                .setId("name")
                .setAccessPolicy(new BlobAccessPolicy());

        String currentMills = String.valueOf(System.currentTimeMillis());
        String containerNameBase = bookBeingReturned.getBook().getTitle() +
                                 "-" + bookBeingReturned.getStudent().getFullName() +
                                 "-" + bookBeingReturned.getStudent().getStudentNumber() +
                                 "-" + LocalDate.now() +
                                 "-" + currentMills;

        var originalContainerName = sanitizeContainerName(containerNameBase);
        var blobContainerClient = blobServiceClient.createBlobContainerIfNotExists(originalContainerName);
        blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER, List.of(blobSignedIdentifier));

        Set<String> computerVisionTags = new HashSet<>();
        Set<String> imagesURLS = new HashSet<>();

        // Create a synchronous client using API key authentication
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
                if (imageBlob.exists()) {
                    log.info("Blob already exists, skipping upload: {}", imageBlob.getBlobUrl());
                    continue;
                }

                log.info("Uploading new image to blob storage: {}", imageBlob.getBlobUrl());
                imageBlob.upload(image.getInputStream());
                String imageUrl = imageBlob.getBlobUrl();

                ImageAnalysisResult result = client.analyzeFromUrl(
                        imageUrl,
                        Collections.singletonList(VisualFeatures.TAGS),
                        new ImageAnalysisOptions().setGenderNeutralCaption(true));

                // Extract just the tag names as strings
                if (result.getTags() != null) {
                    for (var tag : result.getTags().getValues()) {
                        computerVisionTags.add(tag.getName());
                        log.info("Image tag name: {}", tag);
                    }
                }
                imagesURLS.add(imageUrl);
            } catch (IOException e) {
                log.error("Error uploading image to blob storage: {}", e.getMessage(), e);
            }
        }

        // Define tag-specific prices
        Map<String, Double> tagPrices = new HashMap<>();
        tagPrices.put("missing book cover", 90.0);
        tagPrices.put("coffee stain", 20.0);
        tagPrices.put("torn pages", 50.0);
        tagPrices.put("highlighted text", 15.0);
        tagPrices.put("water damage", 15.0);
        tagPrices.put("writing", 10.0);
        tagPrices.put("bent cover", 25.0);
        tagPrices.put("dog ear", 30.0);
        tagPrices.put("loose pages", 40.0);
        tagPrices.put("mold", 80.0);
        tagPrices.put("stains", 20.0);
        tagPrices.put("ripped cover", 70.0);
        tagPrices.put("pages missing", 100.0);
        tagPrices.put("broken spine", 60.0);
        tagPrices.put("damaged cover", 50.0);
        tagPrices.put("torn cover", 70.0);
        tagPrices.put("scratched cover", 30.0);
        tagPrices.put("bent pages", 20.0);
        tagPrices.put("folded pages", 15.0);
        tagPrices.put("underlined text", 10.0);
        tagPrices.put("notes in margins", 25.0);
        tagPrices.put("highlighting", 15.0);
        tagPrices.put("water stains", 40.0);
        tagPrices.put("damp pages", 35.0);
        tagPrices.put("mildew", 75.0);
        tagPrices.put("torn dust jacket", 80.0);
        tagPrices.put("missing dust jacket", 100.0);
        tagPrices.put("damaged dust jacket", 60.0);

        double defaultPrice = 9.99;
        double amountOwed = 0.0;

        Set<String> newTags = new HashSet<>(computerVisionTags);
        for (var tag : bookBeingReturned.getTags()) {
            newTags.removeIf(a -> a.equalsIgnoreCase(tag));
        }

        // Calculate total owed based on tag prices
        for (String tag : newTags) {
            amountOwed += tagPrices.getOrDefault(tag.toLowerCase(), defaultPrice);
        }

        bookBeingReturned.getStudent().setOutstandingFines(amountOwed);
        this.studentService.saveStudent(bookBeingReturned.getStudent());

        // Send email to parents after book is returned
        Student student = bookBeingReturned.getStudent();
        Book book = bookBeingReturned.getBook();
        List<String> parentEmails = student.getEmails().stream()
                .map(Email::getEmail)
                .toList();

        String subject = student.getFullName() + " has returned " + book.getTitle();
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Dear Parent/Guardian,\n\n");
        bodyBuilder.append("We wish to inform you that your child, ")
                .append(student.getFullName())
                .append(" (Student Number: ")
                .append(student.getStudentNumber())
                .append(") has returned the following book:\n\n");
        bodyBuilder.append("Book Title: ").append(book.getTitle()).append("\n");
        bodyBuilder.append("Author: ").append(book.getAuthor() != null ? book.getAuthor() : "N/A").append("\n");
        bodyBuilder.append("Return Date: ").append(LocalDate.now()).append("\n\n");
        if (!imagesURLS.isEmpty()) {
            bodyBuilder.append("Images showing the state of the book at return:\n");
            for (String url : imagesURLS) {
                bodyBuilder.append(url).append("\n");
            }
            bodyBuilder.append("\n");
        }
        if (amountOwed > 0) {
            bodyBuilder.append("Amount owed for damages: R").append(String.format("%.2f", amountOwed)).append("\n");
            bodyBuilder.append("Detected issues: ").append(String.join(", ", newTags)).append("\n\n");
        } else {
            bodyBuilder.append("No fines are owed for this return.\n\n");
        }
        bodyBuilder.append("Regards,\nSeriousApp Team");

        // HTML body
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<p>Dear Parent/Guardian,</p>");
        htmlBuilder.append("<p>We wish to inform you that your child, <strong>")
                .append(student.getFullName())
                .append("</strong> (Student Number: ")
                .append(student.getStudentNumber())
                .append(") has returned the following book:</p>");
        htmlBuilder.append("<ul>");
        htmlBuilder.append("<li><strong>Book Title:</strong> ").append(book.getTitle()).append("</li>");
        htmlBuilder.append("<li><strong>Author:</strong> ").append(book.getAuthor() != null ? book.getAuthor() : "N/A").append("</li>");
        htmlBuilder.append("<li><strong>Return Date:</strong> ").append(LocalDate.now()).append("</li>");
        htmlBuilder.append("</ul>");
        if (!imagesURLS.isEmpty()) {
            htmlBuilder.append("<p>Images showing the state of the book at return:</p><ul>");
            for (String url : imagesURLS) {
                htmlBuilder.append("<li><a href='")
                        .append(url)
                        .append("' target='_blank'>View Image</a></li>");
            }
            htmlBuilder.append("</ul>");
        }
        if (amountOwed > 0) {
            htmlBuilder.append("<p><strong>Amount owed for damages:</strong> R")
                    .append(String.format("%.2f", amountOwed)).append("</p>");
            htmlBuilder.append("<p><strong>Detected issues:</strong> ")
                    .append(String.join(", ", newTags)).append("</p>");
        } else {
            htmlBuilder.append("<p>No fines are owed for this return.</p>");
        }
        htmlBuilder.append("<p>Regards,<br>SeriousApp Team</p>");

        if (!parentEmails.isEmpty()) {
            emailConfiguration.sendEmail(parentEmails, subject, bodyBuilder.toString(), htmlBuilder.toString());
        } else {
            log.warn("No parent emails found for student: {}", student.getFullName());
        }

        return new ResponseEntity<>(amountOwed, HttpStatus.OK);
    }
}


