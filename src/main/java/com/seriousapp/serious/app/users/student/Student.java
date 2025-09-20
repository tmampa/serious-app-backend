package com.seriousapp.serious.app.users.student;

import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.contact.Email;

import com.seriousapp.serious.app.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "students")
@DiscriminatorValue("STUDENT")
public class Student extends User {
    private String firstNames;
    private String lastName;
    private String email;
    private Long studentNumber;
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Email> emails = new HashSet<>();
    private String address;
    private double outstandingFines;
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<BorrowingRecord> borrowedBooks = new ArrayList<>();
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors, getters, setters
    public Student() {}

    public Student(
            String username,
            String email,
            String password,
            String firstNames,
            String lastName,
            String address,
            double outstandingFines,
            Long studentNumber
    ) {
        super(username, email, password);
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.address = address;
        this.outstandingFines = outstandingFines;
        this.studentNumber = studentNumber;
    }
}
