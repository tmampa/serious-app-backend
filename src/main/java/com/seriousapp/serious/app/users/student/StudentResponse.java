package com.seriousapp.serious.app.users.student;

import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.contact.Email;
import com.seriousapp.serious.app.contact.Phone;
import com.seriousapp.serious.app.users.UserRoles;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class StudentResponse {
    private Long id;
    private String fullName;
    private Long studentNumber;
    private String username;
    private UserRoles role;

    private Set<Email> emails;
    private Set<Phone> phoneNumbers;
    private String address;
    private double outstandingFines;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<BorrowingRecord> borrowedBooks = new ArrayList<>();
}
