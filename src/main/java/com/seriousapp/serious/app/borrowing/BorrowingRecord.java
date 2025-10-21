package com.seriousapp.serious.app.borrowing;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.users.student.Student;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "borrowing_record")
public class BorrowingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    @JsonBackReference("student-borrowing")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    @JsonBackReference("book-borrowing")
    private Book book;

    private LocalDate borrowDate;
    private LocalDate returnDate;
    private LocalDate dueDate;

    @ElementCollection
    private Set<String> images = new HashSet<>();

    @ElementCollection
    private Set<String> tags = new HashSet<>();

    private Long barcode;
}
