package com.seriousapp.serious.app.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.seriousapp.serious.app.borrowing.BorrowingRecord;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private Long grade;
    private String barcode;
    private int publishedYear;
    private String publisher;
    private int pages;
    private String language;
    private String genre;
    private String description;
    private double price;
    private String coverImageUrl;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonManagedReference("book-borrowing")
    private List<BorrowingRecord> borrowingRecords = new ArrayList<>();
}
