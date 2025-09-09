package com.seriousapp.serious.app.book;

import org.springframework.stereotype.Service;

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
}
