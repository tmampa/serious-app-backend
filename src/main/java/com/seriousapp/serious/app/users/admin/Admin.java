package com.seriousapp.serious.app.users.admin;

import com.seriousapp.serious.app.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admins")
@DiscriminatorValue("ADMIN")
public class Admin extends User {
    private String employeeId;

    public Admin() {}

    public Admin(String username, String email, String password, String employeeId) {
        super(username, email, password);
        this.employeeId = employeeId;
    }
}
