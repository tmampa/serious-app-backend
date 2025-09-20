package com.seriousapp.serious.app.users.student;

import com.seriousapp.serious.app.parent.ParentRequest;
import lombok.Data;

import java.util.Set;

@Data
public class StudentRequest {
    private String firstNames;
    private String lastName;
    private String email;
    private Long studentNumber;
    private String username;
    private String password;
    private Set<ParentRequest> emails = new java.util.HashSet<>();
    private String address;
    private double outstandingFines;
}
