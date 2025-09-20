package com.seriousapp.serious.app.users.student;

import com.seriousapp.serious.app.contact.Email;
import com.seriousapp.serious.app.contact.EmailRequest;
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
    private Set<EmailRequest> emails = new java.util.HashSet<>();
    private String address;
    private double outstandingFines;
}
