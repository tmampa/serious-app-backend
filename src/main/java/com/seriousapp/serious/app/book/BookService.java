package com.seriousapp.serious.app.book;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        return this.bookRepository.findAll();
    }

    public Optional<Book> findById(Long bookId) {
        return this.bookRepository.findById(bookId);
    }
}
