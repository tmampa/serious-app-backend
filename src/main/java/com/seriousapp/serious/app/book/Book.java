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
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private int publishedYear;
    private String publisher;
    private int pages;
    private String language;
    private String genre;
    private String description;
    private double price;
    private String coverImageUrl;
    private String imagesURL;
    private boolean available = true;
    private List<String> images = new java.util.ArrayList<>();
    private Set<String> tags = new java.util.HashSet<>();
    private double stockQuantity;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonManagedReference("book-borrowing")
    private List<BorrowingRecord> borrowingRecords = new ArrayList<>();
}
