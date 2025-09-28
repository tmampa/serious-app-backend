package com.seriousapp.serious.app.book;

import com.seriousapp.serious.app.dto.BookRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book getBookByName(String name) {
        return bookRepository.findByTitle(name).orElse(null);
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book updateBook(Long bookId, BookRequest bookRequest) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) return null;
        book.setTitle(bookRequest.getTitle());
        book.setAuthor(bookRequest.getAuthor());
        book.setIsbn(bookRequest.getIsbn());
        book.setGrade(bookRequest.getGrade());
        book.setPublishedYear(bookRequest.getPublishedYear());
        book.setPublisher(bookRequest.getPublisher());
        book.setPages(bookRequest.getPages());
        book.setLanguage(bookRequest.getLanguage());
        book.setPrice(bookRequest.getPrice());
        book.setCoverImageUrl(bookRequest.getCoverImageUrl());
        return bookRepository.save(book);
    }
}
