package com.seriousapp.serious.app.borrowing;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.users.student.Student;
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
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
    private double fineAmount;

    private LocalDate borrowDate;
    private LocalDate returnDate;
    private LocalDate dueDate;
    @ElementCollection
    private Set<String> tags = new HashSet<>();
    @ElementCollection
    private Set<String> images = new HashSet<>();
}
