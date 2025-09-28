package com.seriousapp.serious.app.users;

import com.seriousapp.serious.app.jwt.JwtService;
import com.seriousapp.serious.app.jwt.token.AuthenticationResponse;
import com.seriousapp.serious.app.users.student.LoginRequest;
import com.seriousapp.serious.app.users.student.StudentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final UserService userService;
    private final StudentService studentService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody LoginRequest loginRequest) throws AccountNotFoundException {
        Authentication authentication = null;
        try {
            authentication = this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", loginRequest.getEmail(), e);
            throw new RuntimeException(e);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Cast to CustomUserDetails to get access to the User entity
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String jwt = jwtService.generateToken(userDetails);
        return AuthenticationResponse.builder()
                .accessToken(jwt)
                .role(userDetails.getAuthorities().toString())
                .build();

    }

    @GetMapping("/details")
    public UserDetails getCurrentUser(Principal principal) {
        return this.userService.loadUserByUsername(principal.getName());
    }

//    @PutMapping("/{userId}/update-names")
//    public User updateNames(
//            @RequestBody User user,
//            @PathVariable("userId") Long userId
//    ) {
//        return this.userService.updateNames(user, userId);
//    }

    @PutMapping("/{userId}/update-password")
    public User updatePassword(
            @RequestBody User user,
            @PathVariable("userId") Long userId
    ) {
        return this.userService.updatePassword(user, userId);
    }
}
