package com.seriousapp.serious.app.users.student;

import com.seriousapp.serious.app.contact.Email;
import lombok.Data;

import java.util.Set;

@Data
public class StudentRequest {
    private String fullName;
    private Long studentNumber;
    private String username;
    private String password;
    private Set<Email> emails = new java.util.HashSet<>();
    private String address;
    private double outstandingFines;
}
