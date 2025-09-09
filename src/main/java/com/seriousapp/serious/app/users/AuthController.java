package com.seriousapp.serious.app.users;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
public class AuthController {

    private final StudentService studentService;

    public AuthController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> user(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        // Create or update student based on Azure AD B2C information
        Student student = new Student();
        student.setFullName(principal.getName());
        student.setStudentNumber(Long.parseLong(principal.getPreferredUsername().split("@")[0]));

        // Save the student
        studentService.saveStudent(student);

        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "name", principal.getName(),
            "email", principal.getEmail(),
            "attributes", principal.getClaims()
        ));
    }
}
