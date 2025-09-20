package com.seriousapp.serious.app.parent;

import com.seriousapp.serious.app.users.student.Student;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

@Data
@Entity
@Table(name = "parents")
public class Parent {
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
        if (o == null || getClass() != o.getClass()) return false;
        Parent parent = (Parent) o;
        return Objects.equals(id, parent.id) && Objects.equals(email, parent.email) && Objects.equals(name, parent.name) && Objects.equals(relationship, parent.relationship) && Objects.equals(student, parent.student);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, name, relationship, student);
    }

    @Override
    public String toString() {
        return "Parent{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", relationship='" + relationship + '\'' +
                ", student=" + student +
                '}';
    }
}
