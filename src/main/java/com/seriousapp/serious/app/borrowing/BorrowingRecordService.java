package com.seriousapp.serious.app.borrowing;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.users.student.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BorrowingRecordService {
    private final BorrowingRecordRepository borrowingRecordRepository;

    public BorrowingRecordService(BorrowingRecordRepository borrowingRecordRepository) {
        this.borrowingRecordRepository = borrowingRecordRepository;
    }

    public BorrowingRecord save(BorrowingRecord borrowingRecord) {
        return this.borrowingRecordRepository.save(borrowingRecord);
    }

    public Optional<BorrowingRecord> findByStudentAndBookAndReturnDateIsNull(Student student, Book book) {
        return this.borrowingRecordRepository.findByStudentAndBookAndReturnDateIsNull(student, book);
    }

    public List<BorrowingRecord> findByStudentAndReturnDateIsNull(Student student) {
        return this.borrowingRecordRepository.findByStudentAndReturnDateIsNull(student);
    }

    public Optional<BorrowingRecord> findById(Long recordId) {
        try {
            return this.borrowingRecordRepository.findById(recordId);
        } catch (Exception e) {
            throw new RuntimeException("Error finding borrowing record by ID", e);
        }
    }
}
