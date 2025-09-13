package com.seriousapp.serious.app.users.admin;

import com.seriousapp.serious.app.users.User;
import com.seriousapp.serious.app.users.UserRoles;
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

    public Admin() {}

    public Admin(UserRoles userRoles, String username, String password) {
        this.role = userRoles;
        this.username = username;
        this.password = password;
    }
}
