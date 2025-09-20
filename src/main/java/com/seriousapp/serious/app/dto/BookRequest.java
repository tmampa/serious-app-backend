package com.seriousapp.serious.app.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class BookRequest {
    private String title;
    private String author;
    private String isbn;
    private int publishedYear;
    private String publisher;
    private int pages;
    private String language;
    private String description;
    private double price;
    private String coverImageUrl;
    private Long grade;
    private String genre;
    private String barcode;

}
