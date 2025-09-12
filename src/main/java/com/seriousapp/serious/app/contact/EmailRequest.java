package com.seriousapp.serious.app.contact;

import lombok.Data;

@Data
public class EmailRequest {
    private String email;
    private String name;
    private String relationship;
}
