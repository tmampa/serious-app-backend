package com.seriousapp.serious.app.users.student;

import com.seriousapp.serious.app.dto.BorrowRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Student", description = "Student API endpoints")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(
        summary = "Get student profile",
        description = "Retrieves the profile information of the currently authenticated student"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved student profile")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/profile")
    public ResponseEntity<Student> getProfile(@AuthenticationPrincipal OidcUser principal) {
        Student student = studentService.findByEmail(principal.getEmail());
        return ResponseEntity.ok(student);
    }

    @Operation(
        summary = "Get borrowed books",
        description = "Retrieves the list of books currently borrowed by the student"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved borrowed books")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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

    @Operation(
        summary = "Get outstanding fines",
        description = "Retrieves the total amount of outstanding fines for the student"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved outstanding fines")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/outstanding-fines")
    public ResponseEntity<Map<String, Double>> getOutstandingFines(@AuthenticationPrincipal OidcUser principal) {
        Student student = studentService.findByEmail(principal.getEmail());
        return ResponseEntity.ok(Map.of("outstandingFines", student.getOutstandingFines()));
    }

    @Operation(
        summary = "Get borrowing history",
        description = "Retrieves the borrowing history of the student, including past borrowed books"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved borrowing history")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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
