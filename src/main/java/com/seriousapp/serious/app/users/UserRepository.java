package com.seriousapp.serious.app.users;


import com.seriousapp.serious.app.users.admin.Admin;
import com.seriousapp.serious.app.users.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.userType = 'ADMIN' AND u.username = :username")
    Optional<Admin> findAdminByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.userType = 'STUDENT' AND u.username = :username")
    Optional<Student> findStudentByUsername(String username);
}
