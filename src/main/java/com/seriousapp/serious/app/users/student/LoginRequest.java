package com.seriousapp.serious.app.users.student;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
