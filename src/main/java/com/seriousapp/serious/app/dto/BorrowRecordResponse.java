package com.seriousapp.serious.app.dto;

import lombok.Data;

import java.time.LocalDate;

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
}
