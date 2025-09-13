package com.seriousapp.serious.app.users.admin;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.book.BookService;
import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.borrowing.BorrowingRecordService;
import com.seriousapp.serious.app.contact.Email;
import com.seriousapp.serious.app.dto.BookRequest;
import com.seriousapp.serious.app.dto.BorrowRecordResponse;
import com.seriousapp.serious.app.dto.UserRequest;
import com.seriousapp.serious.app.users.student.Student;
import com.seriousapp.serious.app.users.student.StudentRequest;
import com.seriousapp.serious.app.users.student.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin management API endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private final AdminService adminService;
    private final BookService bookService;
    private final BorrowingRecordService borrowingRecordService;
    private final StudentService studentService;

    public AdminController(AdminService adminService,
                         BookService bookService,
                         BorrowingRecordService borrowingRecordService,
                         StudentService studentService) {
        this.adminService = adminService;
        this.bookService = bookService;
        this.borrowingRecordService = borrowingRecordService;
        this.studentService = studentService;
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
        book.setAvailable(true);
        return ResponseEntity.ok(bookService.saveBook(book));
    }

    @Operation(
        summary = "Create a borrowing record",
        description = "Records a book borrowing transaction for a student"
    )
    @ApiResponse(responseCode = "200", description = "Borrowing record successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request or book not available")
    @ApiResponse(responseCode = "404", description = "Book or user not found")
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

    @PostMapping
    public void registerNewAdmin(Admin admin){
        adminService.addNewAdmin(admin);
    }

    @PostMapping("/create-student")
    public ResponseEntity<?> registerNewStudent(@RequestBody StudentRequest studentRequest){
        Student student = new Student();
        Set<Email> emailSet = new java.util.HashSet<>();

        student.setFullName(studentRequest.getFullName());
        student.setStudentNumber(studentRequest.getStudentNumber());
        student.setUsername(studentRequest.getUsername());
        student.setPassword(studentRequest.getPassword());
        student.setAddress(studentRequest.getAddress());
        student.setOutstandingFines(studentRequest.getOutstandingFines());
        studentRequest.getEmails().forEach(email -> {
            Email newEmail = new Email();
            newEmail.setEmail(email.getEmail());
            newEmail.setStudent(student);
            newEmail.setRelationship(email.getRelationship());
            newEmail.setName(email.getName());
            emailSet.add(newEmail);
        });
        student.setEmails(emailSet);
        student.setPhoneNumbers(studentRequest.getPhoneNumbers());
        var savedStudent = studentService.createStudent(student);
        return ResponseEntity.ok(savedStudent);
    }

    @PostMapping("/create-admin")
    public ResponseEntity<?> registerNewAdmin(@RequestBody AdminRequest adminRequest){
        Admin admin = new Admin();
        admin.setFullName(adminRequest.getFullName());
        admin.setUsername(adminRequest.getUsername());
        admin.setPassword(adminRequest.getPassword());
        admin.setEmail(adminRequest.getEmail());
        admin.setEmployeeId(adminRequest.getEmployeeId());
        var savedAdmin = adminService.createAdmin(admin);
        return ResponseEntity.ok(savedAdmin);
    }
}
