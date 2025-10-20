package com.seriousapp.serious.app.dto;

import java.time.LocalDate;
import java.util.Set;

import com.seriousapp.serious.app.parent.ParentRequest;
import lombok.Data;

@Data
public class UserRequest {
    private String fullName;
    private Long studentNumber;
    private Set<ParentRequest> emails = new java.util.HashSet<>();
    private String address;
    private LocalDate returnDate;
    private long barcode;
}
