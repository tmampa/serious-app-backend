package com.seriousapp.serious.app.users.admin;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.book.BookResponse;
import com.seriousapp.serious.app.book.BookService;
import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.borrowing.BorrowingRecordService;
import com.seriousapp.serious.app.configurations.EmailConfiguration;
import com.seriousapp.serious.app.parent.Parent;
import com.seriousapp.serious.app.dto.BookRequest;
import com.seriousapp.serious.app.dto.BorrowRecordResponse;
import com.seriousapp.serious.app.dto.UserRequest;
import com.seriousapp.serious.app.parent.ParentResponse;
import com.seriousapp.serious.app.users.student.Student;
import com.seriousapp.serious.app.users.student.StudentRequest;
import com.seriousapp.serious.app.users.student.StudentResponse;
import com.seriousapp.serious.app.users.student.StudentService;
import com.seriousapp.serious.app.utils.PasswordGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/admin")
//@PreAuthorize("hasRole('ADMIN')")
//@Tag(name = "Admin", description = "Admin management API endpoints")
//@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminService adminService;
    private final BookService bookService;
    private final BorrowingRecordService borrowingRecordService;
    private final StudentService studentService;
    private final EmailConfiguration emailConfiguration;
    private final PasswordGenerator passwordGenerator;

    public AdminController(AdminService adminService,
                         BookService bookService,
                         BorrowingRecordService borrowingRecordService,
                         StudentService studentService,
                         EmailConfiguration emailConfiguration,
                         PasswordGenerator passwordGenerator) {
        this.adminService = adminService;
        this.bookService = bookService;
        this.borrowingRecordService = borrowingRecordService;
        this.studentService = studentService;
        this.emailConfiguration = emailConfiguration;
        this.passwordGenerator = passwordGenerator;
    }

    @Operation(
        summary = "Create a new book",
        description = "Adds a new book to the library system"
    )
    @ApiResponse(responseCode = "200", description = "Book successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid book data provided")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    @PostMapping("/books")
    public ResponseEntity<Book> createBook(
            @Parameter(description = "Book details", required = true)
            @RequestBody BookRequest bookRequest) {
        Book book = new Book();
        book.setTitle(bookRequest.getTitle());
        book.setAuthor(bookRequest.getAuthor());
        book.setIsbn(bookRequest.getIsbn());
        book.setGrade(bookRequest.getGrade());
        book.setBarcode(bookRequest.getBarcode());
        book.setPublishedYear(bookRequest.getPublishedYear());
        book.setPublisher(bookRequest.getPublisher());
        book.setPages(bookRequest.getPages());
        book.setLanguage(bookRequest.getLanguage());
        book.setGenre(bookRequest.getGenre());
        book.setDescription(bookRequest.getDescription());
        book.setPrice(bookRequest.getPrice());
        book.setCoverImageUrl(bookRequest.getCoverImageUrl());
        return ResponseEntity.ok(bookService.saveBook(book));
    }

    @Operation(
        summary = "Create a borrowing record",
        description = "Records a book borrowing transaction for a student"
    )
    @ApiResponse(responseCode = "200", description = "Borrowing record successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request or book not available")
    @ApiResponse(responseCode = "404", description = "Book or userPrincipal not found")
    @PostMapping("/books/borrow/{bookTitle}")
    public ResponseEntity<BorrowRecordResponse> borrowBook(
            @Parameter(description = "Title of the book to borrow", required = true)
            @PathVariable String bookTitle,
            @Parameter(description = "User details", required = true)
            @RequestBody UserRequest userRequest) {
        BorrowRecordResponse response = adminService.createBorrowRecord(userRequest, bookTitle);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/books/upload-images/{recordId}")
    public ResponseEntity<BorrowingRecord> uploadBorrowImages(
            @PathVariable Long recordId,
            @RequestParam("images") List<MultipartFile> images) {
        return ResponseEntity.ok(adminService.uploadBorrowImages(recordId, images));
    }

    @PostMapping("/books/return/{studentNumber}/{bookTitle}")
    public ResponseEntity<Double> returnBook(
            @PathVariable Long studentNumber,
            @PathVariable String bookTitle,
            @RequestParam("images") List<MultipartFile> images) {
        double fineAmount = adminService.returnBook(studentNumber, bookTitle, images);
        return ResponseEntity.ok(fineAmount);
    }

    @GetMapping("/students/{studentId}/fines")
    public ResponseEntity<Double> getStudentFines(@PathVariable Long studentId) {
        Student student = studentService.findById(studentId);
        return ResponseEntity.ok(student.getOutstandingFines());
    }

    @PutMapping("/students/{studentId}/fines/clear")
    public ResponseEntity<Void> clearStudentFines(@PathVariable Long studentId) {
        studentService.clearFines(studentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-student")
    public ResponseEntity<?> registerNewStudent(@RequestBody StudentRequest studentRequest) {
        Student student = new Student();
        String generatedPassword = passwordGenerator.generateSecurePassword();

        // Set User properties first
        student.setUsername(studentRequest.getEmail());
        student.setPassword(generatedPassword); // Use generated password instead
        student.setEmail(studentRequest.getEmail());

        // Set Student-specific properties
        student.setFirstNames(studentRequest.getFirstNames());
        student.setLastName(studentRequest.getLastName());

        long generatedStudentNumber = 1_000_000_000L + (long) (Math.random() * 9_000_000_000L);

        log.info("Generated new student number: " + generatedStudentNumber);

        student.setStudentNumber(generatedStudentNumber);
        student.setAddress(studentRequest.getAddress());
        student.setOutstandingFines(studentRequest.getOutstandingFines());

        // Handle parents
        Set<Parent> parentSet = new java.util.HashSet<>();
        studentRequest.getParents().forEach(parent -> {
            Parent newParent = new Parent();
            newParent.setEmail(parent.getEmail());
            newParent.setStudent(student);
            newParent.setRelationship(parent.getRelationship());
            newParent.setName(parent.getName());
            parentSet.add(newParent);
        });
        student.setParents(parentSet);

        var savedStudent = studentService.saveStudent(student);

        // Send email to student
        sendLoginCredentialsEmail(
            List.of(student.getEmail()),
            student.getUsername(),
            generatedPassword,
            "Student"
        );

        // Send email to parents
        List<String> parentEmails = savedStudent.getParents().stream()
            .map(Parent::getEmail)
            .toList();
        if (!parentEmails.isEmpty()) {
            sendLoginCredentialsEmail(
                parentEmails,
                student.getUsername(),
                generatedPassword,
                "Parent"
            );
        }

        // Convert to StudentResponse
        Set<ParentResponse> parentResponses = savedStudent.getParents().stream()
            .map(parent -> ParentResponse.builder()
                .id(parent.getId())
                .name(parent.getName())
                .email(parent.getEmail())
                .relationship(parent.getRelationship())
                .build())
            .collect(java.util.stream.Collectors.toSet());

        StudentResponse response = StudentResponse.builder()
            .id(savedStudent.getId())
            .fullName(savedStudent.getFirstNames() + " " + savedStudent.getLastName())
            .studentNumber(savedStudent.getStudentNumber())
            .username(savedStudent.getUsername())
            .role("STUDENT")
            .parents(parentResponses)
            .address(savedStudent.getAddress())
            .outstandingFines(savedStudent.getOutstandingFines())
            .borrowedBooks(savedStudent.getBorrowedBooks())
            .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/student/{studentId}")
    public ResponseEntity<?> updateStudent(
            @PathVariable Long studentId,
            @RequestBody StudentRequest studentRequest) {
        Student student = studentService.findById(studentId);
        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        student.setUsername(studentRequest.getEmail());
        student.setEmail(studentRequest.getEmail());
        student.setFirstNames(studentRequest.getFirstNames());
        student.setLastName(studentRequest.getLastName());
        student.setAddress(studentRequest.getAddress());
        student.setOutstandingFines(studentRequest.getOutstandingFines());

        // Update parents
        Set<Parent> newParents = new java.util.HashSet<>();
        if (studentRequest.getParents() != null) {
            studentRequest.getParents().forEach(parentDto -> {
                Parent parent = new Parent();
                parent.setEmail(parentDto.getEmail());
                parent.setName(parentDto.getName());
                parent.setRelationship(parentDto.getRelationship());
                parent.setStudent(student);
                newParents.add(parent);
            });
        }
        student.setParents(newParents);

        Student updatedStudent = studentService.saveStudent(student);

        Set<ParentResponse> parentResponses = updatedStudent.getParents().stream()
            .map(parent -> ParentResponse.builder()
                .id(parent.getId())
                .name(parent.getName())
                .email(parent.getEmail())
                .relationship(parent.getRelationship())
                .build())
            .collect(java.util.stream.Collectors.toSet());

        StudentResponse response = StudentResponse.builder()
            .id(updatedStudent.getId())
            .fullName(updatedStudent.getFirstNames() + " " + updatedStudent.getLastName())
            .studentNumber(updatedStudent.getStudentNumber())
            .username(updatedStudent.getUsername())
            .role("STUDENT")
            .parents(parentResponses)
            .address(updatedStudent.getAddress())
            .outstandingFines(updatedStudent.getOutstandingFines())
            .borrowedBooks(updatedStudent.getBorrowedBooks())
            .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-admin")
    public ResponseEntity<?> registerNewAdmin(@RequestBody AdminRequest adminRequest){
        Admin admin = new Admin();
        admin.setUsername(adminRequest.getUsername());
        admin.setPassword(adminRequest.getPassword());
        admin.setEmail(adminRequest.getEmail());
        admin.setEmployeeId(adminRequest.getEmployeeId());
        var savedAdmin = adminService.createAdmin(admin);
        return ResponseEntity.ok(savedAdmin);
    }

    @GetMapping("/books")
    public ResponseEntity<?> getAllBooks() {

        var books = this.bookService.getAllBooks();
        List<BookResponse> bookResponses = new ArrayList<>();

        for (Book book: books) {
            bookResponses.add(BookResponse.builder()
                    .id(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .isbn(book.getIsbn())
                    .build());
        }

        return ResponseEntity.ok(bookResponses);
    }

    @GetMapping("/borrow-records")
    public ResponseEntity<?> getAllBorrowRecords() {
        var records = this.borrowingRecordService.getAllBorrowRecords();
        List<BorrowRecordResponse> borrowingRecords = new ArrayList<>();

        for (BorrowingRecord record: records) {
            BorrowRecordResponse borrowRecordResponse = new BorrowRecordResponse();
            borrowRecordResponse.setId(record.getId());
            borrowRecordResponse.setStudentId(record.getStudent().getId());
            borrowRecordResponse.setStudentName(record.getStudent().getFirstNames() + " " + record.getStudent().getLastName());
            borrowRecordResponse.setStudentNumber(String.valueOf(record.getStudent().getStudentNumber()));
            borrowRecordResponse.setBookTitle(record.getBook().getTitle());
            borrowRecordResponse.setBorrowDate(record.getBorrowDate());
           // borrowRecordResponse.setDueDate(record.getDueDate());
            borrowRecordResponse.setReturnDate(record.getReturnDate());
            //borrowRecordResponse.setFineAmount(record.getFineAmount());
            borrowingRecords.add(borrowRecordResponse);
        }

        return ResponseEntity.ok(borrowingRecords);
    }

    @GetMapping("/students")
    public ResponseEntity<?> getAllStudents() {
        var students = this.studentService.getAllStudents();
        List<StudentResponse> studentResponses = new ArrayList<>();

        for (Student student: students) {
            // Convert Parent entities to ParentResponse DTOs
            Set<ParentResponse> parentResponses = student.getParents().stream()
                .map(parent -> ParentResponse.builder()
                    .id(parent.getId())
                    .name(parent.getName())
                    .email(parent.getEmail())
                    .relationship(parent.getRelationship())
                    .build())
                .collect(java.util.stream.Collectors.toSet());

            studentResponses.add(StudentResponse.builder()
                    .id(student.getId())
                    .fullName(student.getFirstNames() + " " + student.getLastName())
                    .studentNumber(student.getStudentNumber())
                    .username(student.getUsername())
                    .role("STUDENT")
                    .parents(parentResponses)
                    .address(student.getAddress())
                    .outstandingFines(student.getOutstandingFines())
                    .borrowedBooks(student.getBorrowedBooks())
                    .build());
        }
        return ResponseEntity.ok(studentResponses);
    }

    private void sendLoginCredentialsEmail(List<String> emails, String username, String password, String recipientType) {
        String subject = "Library System - Login Credentials";
        String plainText = String.format("""
            Welcome to the Library System!
            
            Here are the login credentials for %s:
            Username: %s
            Password: %s
            
            Please change your password upon first login.
            
            Best regards,
            Library System Team
            """, recipientType, username, password);

        String htmlBody = String.format("""
            <html>
            <body>
                <h2>Welcome to the Library System!</h2>
                <p>Here are the login credentials for %s:</p>
                <p><strong>Username:</strong> %s</p>
                <p><strong>Password:</strong> %s</p>
                <p>Please change your password upon first login.</p>
                <br>
                <p>Best regards,<br>Library System Team</p>
            </body>
            </html>
            """, recipientType, username, password);

        emailConfiguration.sendEmail(emails, subject, plainText, htmlBody);
    }
}
