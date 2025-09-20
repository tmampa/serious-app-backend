package com.seriousapp.serious.app.users.student;

import com.seriousapp.serious.app.borrowing.BorrowingRecord;
import com.seriousapp.serious.app.parent.Parent;
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
    private String role;
    private Set<Parent> parents;
    private String address;
    private double outstandingFines;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<BorrowingRecord> borrowedBooks = new ArrayList<>();
}
