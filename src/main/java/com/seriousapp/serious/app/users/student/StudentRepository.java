package com.seriousapp.serious.app.users.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query(value = """
            SELECT s.*, u.* 
            FROM students s 
            JOIN users u ON s.id = u.id 
            WHERE s.student_number = :studentNumber
            """, nativeQuery = true)
    Optional<Student> findByStudentNumber(@Param("studentNumber") Long studentNumber);

    @Query(value = """
            SELECT s.*, u.* 
            FROM students s 
            JOIN users u ON s.id = u.id 
            WHERE CONCAT(s.first_names, ' ', s.last_name) = :fullName
            """, nativeQuery = true)
    Optional<Student> findByFullName(@Param("fullName") String fullName);

    @Query(value = "SELECT * FROM students s JOIN users u on s.id = u.id WHERE s.email = :email", nativeQuery = true)
    Student findByEmail(@Param("email") String email);

    Student findByUsername(String username);
}
