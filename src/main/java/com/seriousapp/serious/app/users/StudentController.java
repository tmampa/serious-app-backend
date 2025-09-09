package com.seriousapp.serious.app.users;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobServiceClient;
import com.seriousapp.serious.app.dto.UserRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/student")
@RestController
public class StudentController {
    private final StudentService studentService;
    private final BlobServiceClient blobServiceClient;

    public StudentController(StudentService studentService, BlobServiceClient blobServiceClient) {
        this.studentService = studentService;
        this.blobServiceClient = blobServiceClient;
    }

    @PostMapping(value = "/borrow-book/{book_name}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public void borrowBook(
            @RequestBody UserRequest userRequest,
            @PathVariable("book_name") String bookName,
            @RequestParam("images") List<MultipartFile> images) throws IOException, IllegalAccessException {

        this.studentService.borrowBook(userRequest, bookName, images);
    }

    @PostMapping("/return-book")
    public void returnBook() {
    }

    @GetMapping("/all")
    public ResponseEntity<List<Student>> getAllUsers() {
        return ResponseEntity.ok(this.studentService.getAllUsers());
    }

    @PostMapping("/settle-fines")
    public ResponseEntity<?> settleFines() {
        return ResponseEntity.ok("Fines settled successfully");
    }
}
