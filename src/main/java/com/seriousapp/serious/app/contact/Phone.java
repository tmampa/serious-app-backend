package com.seriousapp.serious.app.contact;

import com.seriousapp.serious.app.users.student.Student;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

@Data
@Entity
@Table(name = "phone")
public class Phone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String number;
    private String name;
    private String relationship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", referencedColumnName = "id")
    private Student student;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return Objects.equals(id, phone.id) && Objects.equals(number, phone.number) && Objects.equals(name, phone.name) && Objects.equals(relationship, phone.relationship) && Objects.equals(student, phone.student);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number, name, relationship, student);
    }

    @Override
    public String toString() {
        return "Phone{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", name='" + name + '\'' +
                ", relationship='" + relationship + '\'' +
                ", student=" + student +
                '}';
    }
}
