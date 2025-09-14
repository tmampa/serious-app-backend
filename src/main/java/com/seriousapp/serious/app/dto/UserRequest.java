package com.seriousapp.serious.app.dto;

import java.util.Set;

import com.seriousapp.serious.app.contact.Email;

import lombok.Data;

@Data
public class UserRequest {
    private String fullName;
    private Long studentNumber;
    private Set<Email> emails = new java.util.HashSet<>();
    private Set<Phone> phoneNumbers = new java.util.HashSet<>();
    private String address;
}
