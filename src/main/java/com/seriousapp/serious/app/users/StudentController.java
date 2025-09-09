package com.seriousapp.serious.app.users;

import com.seriousapp.serious.app.dto.BorrowRecordResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/profile")
    public ResponseEntity<Student> getProfile(@AuthenticationPrincipal OidcUser principal) {
        Student student = studentService.findByEmail(principal.getEmail());
        return ResponseEntity.ok(student);
    }

    @GetMapping("/borrowed-books")
    public ResponseEntity<List<BorrowRecordResponse>> getBorrowedBooks(@AuthenticationPrincipal OidcUser principal) {
        Student student = studentService.findByEmail(principal.getEmail());
        List<BorrowRecordResponse> borrowedBooks = student.getBorrowedBooks().stream()
            .map(record -> new BorrowRecordResponse(
                record.getBook().getTitle(),
                record.getBorrowDate(),
                record.getDueDate(),
                record.getReturnDate(),
                record.getFineAmount()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(borrowedBooks);
    }

    @GetMapping("/outstanding-fines")
    public ResponseEntity<Map<String, Double>> getOutstandingFines(@AuthenticationPrincipal OidcUser principal) {
        Student student = studentService.findByEmail(principal.getEmail());
        return ResponseEntity.ok(Map.of("outstandingFines", student.getOutstandingFines()));
    }

    @GetMapping("/borrowing-history")
    public ResponseEntity<List<BorrowRecordResponse>> getBorrowingHistory(@AuthenticationPrincipal OidcUser principal) {
        Student student = studentService.findByEmail(principal.getEmail());
        List<BorrowRecordResponse> history = student.getBorrowedBooks().stream()
            .map(record -> new BorrowRecordResponse(
                record.getBook().getTitle(),
                record.getBorrowDate(),
                record.getDueDate(),
                record.getReturnDate(),
                record.getFineAmount()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }
}
