package com.seriousapp.serious.app.borrowing;

import com.seriousapp.serious.app.book.Book;
import com.seriousapp.serious.app.users.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {
    Optional<BorrowingRecord> findByStudentAndBookAndReturnDateIsNull(Student student, Book book);
    List<BorrowingRecord> findByStudentAndReturnDateIsNull(Student student);
}
