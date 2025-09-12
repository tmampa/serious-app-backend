package com.seriousapp.serious.app.users;

import com.seriousapp.serious.app.jwt.token.AuthenticationResponse;
import com.seriousapp.serious.app.users.student.StudentService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final UserService userService;
    private final StudentService studentService;

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody User user) throws AccountNotFoundException {
        return this.userService.authenticate(user);

    }

    @GetMapping("/details")
    public UserDetails getCurrentUser(Principal principal) {
        return this.userService.loadUserByUsername(principal.getName());
    }

    @PutMapping("/{userId}/update-names")
    public User updateNames(
            @RequestBody User user,
            @PathVariable("userId") Long userId
    ) {
        return this.userService.updateNames(user, userId);
    }

    @PutMapping("/{userId}/update-password")
    public User updatePassword(
            @RequestBody User user,
            @PathVariable("userId") Long userId
    ) {
        return this.userService.updatePassword(user, userId);
    }
}
