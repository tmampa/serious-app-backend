package com.seriousapp.serious.app.users;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "admin")
public class Admin extends User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String email;
    private String employeeId;
    private String username;
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRoles role = UserRoles.ADMIN;
}
