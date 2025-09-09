package com.seriousapp.serious.app.book;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobAccessPolicy;
import com.azure.storage.blob.models.BlobSignedIdentifier;
import com.azure.storage.blob.models.PublicAccessType;
import com.seriousapp.serious.app.dto.BookRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bookService;
    private final BlobServiceClient blobServiceClient;

    public BookController(BookService bookService, BlobServiceClient blobServiceClient) {
        this.bookService = bookService;
        this.blobServiceClient = blobServiceClient;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Book>> getAllBooks() {
        return new ResponseEntity<>(this.bookService.getAllBooks(), HttpStatus.OK);
    }

    @PostMapping("/create-book")
    public ResponseEntity<?> addBook(
            @RequestBody BookRequest request
    ) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setPublisher(request.getPublisher());
        book.setPages(request.getPages());
        book.setLanguage(request.getLanguage());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());

        book.setStockQuantity(request.getStockQuantity());

        this.bookService.saveBook(book);

        return ResponseEntity.status(201).body("Book added successfully");
    }

    @PutMapping(
            value = "/upload-book-cover/{book_id}",
            consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE }
    )
    public ResponseEntity<?> updateBookCover(@PathVariable("book_id") Long bookId, @RequestParam("image") MultipartFile image) {
        Optional<Book> book = this.bookService.findById(bookId);

        if (book.isEmpty()) {
            return new ResponseEntity<>("Book not found with id:" + bookId, HttpStatus.NOT_FOUND);
        }

        // add blob storage upload

        BlobSignedIdentifier blobSignedIdentifier = new BlobSignedIdentifier()
                .setId("name")
                .setAccessPolicy(new BlobAccessPolicy());

        var originalContainerName = book.get().getTitle().replaceAll("\\s+", "-").toLowerCase();
        var blobContainerClient = this.blobServiceClient.createBlobContainerIfNotExists(originalContainerName);
        blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER, List.of(blobSignedIdentifier));

        var imageBlob = blobContainerClient.getBlobClient(image.getOriginalFilename()
                .replaceAll("\\s+", "-").toLowerCase());

        try {
            imageBlob.upload(image.getInputStream());
            String imageUrl = imageBlob.getBlobUrl();
            book.get().setCoverImageUrl(imageUrl);
        } catch (IOException e) {
            log.error("Error uploading image to blob storage: {}", e.getMessage(), e);
        }

        this.bookService.saveBook(book.get());

        return new ResponseEntity<>("Cover updated successfully", HttpStatus.OK);
    }
}
