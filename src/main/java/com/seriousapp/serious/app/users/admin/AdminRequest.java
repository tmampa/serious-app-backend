package com.seriousapp.serious.app.users.admin;

import lombok.Data;

@Data
public class AdminRequest {
    private String fullName;
    private String email;
    private String employeeId;
    private String username;
    private String password;
}
