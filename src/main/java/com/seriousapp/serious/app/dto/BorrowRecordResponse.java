package com.seriousapp.serious.app.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class BorrowRecordResponse {
    private Long id;
    // Student details
    private Long studentId;
    private String studentName;
    private String studentNumber;
    // Book details
    private Long bookId;
    private String bookTitle;
    private String author;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;
    private long barcode;
    private Set<String> images = new HashSet<>();
    private Set<String> tags = new HashSet<>();

    public BorrowRecordResponse() {}

    public BorrowRecordResponse(String title, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, double fineAmount) {
        this.bookTitle = title;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
    }
}
