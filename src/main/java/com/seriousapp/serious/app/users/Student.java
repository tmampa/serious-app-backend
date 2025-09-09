package com.seriousapp.serious.app.users;

import java.util.*;

import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.contact.Email;
import com.seriousapp.serious.app.contact.Phone;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private Long studentNumber;
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Email> emails = new java.util.HashSet<>();
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Phone> phoneNumbers = new java.util.HashSet<>();
    private String address;
    private double outstandingFines;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<BorrowingRecord> borrowedBooks = new ArrayList<>();

    // Helper methods for bidirectional relationship
    public void addChild(Email email) {
        emails.add(email);
        email.setStudent(this);
    }

    public void removeChild(Email email) {
        emails.remove(email);
        email.setStudent(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(id, student.id) &&
               Objects.equals(fullName, student.fullName) &&
               Objects.equals(studentNumber, student.studentNumber) &&
               Objects.equals(address, student.address) &&
               Double.compare(outstandingFines, student.outstandingFines) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fullName, studentNumber, address, outstandingFines);
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", studentNumber=" + studentNumber +
                ", address='" + address + '\'' +
                ", outstandingFines=" + outstandingFines +
                '}';
    }
}
