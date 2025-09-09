package com.seriousapp.serious.app.users;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.book.BookService;
import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.borrowing.BorrowingRecordService;
import com.seriousapp.serious.app.dto.BookRequest;
import com.seriousapp.serious.app.dto.BorrowRecordResponse;
import com.seriousapp.serious.app.dto.UserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/books")
    public ResponseEntity<Book> createBook(@RequestBody BookRequest bookRequest) {
        Book book = new Book();
        book.setTitle(bookRequest.getTitle());
        book.setAuthor(bookRequest.getAuthor());
        book.setIsbn(bookRequest.getIsbn());
        book.setAvailable(true);
        return ResponseEntity.ok(bookService.saveBook(book));
    }

    @PostMapping("/books/borrow/{bookTitle}")
    public ResponseEntity<BorrowRecordResponse> borrowBook(
            @PathVariable String bookTitle,
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
}
