package com.seriousapp.serious.app.contact;

import com.seriousapp.serious.app.users.Student;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

@Data
@Entity
@Table(name = "email")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String name;
    private String relationship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", referencedColumnName = "id")
    private Student student;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email1 = (Email) o;
        return Objects.equals(id, email1.id) &&
               Objects.equals(email, email1.email) &&
               Objects.equals(name, email1.name) &&
               Objects.equals(relationship, email1.relationship);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, name, relationship);
    }

    @Override
    public String toString() {
        return "Email{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", relationship='" + relationship + '\'' +
                '}';
    }
}
