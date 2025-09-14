package com.seriousapp.serious.app.users.student;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
