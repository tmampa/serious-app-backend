package com.seriousapp.serious.app.dto;

import java.util.Set;

import com.seriousapp.serious.app.contact.Email;

import com.seriousapp.serious.app.contact.EmailRequest;
import lombok.Data;

@Data
public class UserRequest {
    private String fullName;
    private Long studentNumber;
    private Set<EmailRequest> emails = new java.util.HashSet<>();
    private String address;
}
