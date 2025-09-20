package com.seriousapp.serious.app.users.admin;

import com.seriousapp.serious.app.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "admins")
@DiscriminatorValue("ADMIN")
public class Admin extends User {
    private String employeeId;
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Admin() {}

    public Admin(String username, String email, String password, String employeeId) {
        super(username, email, password);
        this.employeeId = employeeId;
    }
}
