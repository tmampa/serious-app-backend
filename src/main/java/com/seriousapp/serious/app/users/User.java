package com.seriousapp.serious.app.users;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, unique = true, name = "id")
    private Long id;
    @Column(length = 25, name = "first_name")
    private String firstName;
    @Column(length = 25, name = "last_name")
    private String lastName;
    @Column(unique = true, nullable = false, name = "email")
    private String email;
    @Column(nullable = false, name = "password")
    private String password;
    @Column(name = "phone")
    private Long phone;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRoles role;
    @Column(name = "display_picture_url")
    private String displayPictureURL;
    @Column(name = "description")
    private String description;

    public User(UserRoles role, String username, String password) {
        this.role = role;
        this.email = username;
        this.password = password;
    }

    public User(
            String firstName,
            String lastName,
            UserRoles userRoles,
            String username,
            String password
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = userRoles;
        this.email = username;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(role);
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
