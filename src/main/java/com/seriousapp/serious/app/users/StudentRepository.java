package com.seriousapp.serious.app.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query(value = "select * from student where full_name = :fullName", nativeQuery = true)
    Optional<Student> findByFullName(@Param("fullName") String fullName);

    @Query(value = "select * from student where student_number = :studentNumber", nativeQuery = true)
    Optional<Student> findByStudentNumber(@Param("studentNumber") Long studentNumber);

    @Query("SELECT s FROM Student s JOIN s.emails e WHERE e.email = :email")
    Student findByEmail(@Param("email") String email);

    Student findByUsername(String username);
}
