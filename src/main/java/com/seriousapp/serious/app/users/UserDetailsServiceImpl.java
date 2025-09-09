package com.seriousapp.serious.app.users;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;

    public UserDetailsServiceImpl(StudentRepository studentRepository, AdminRepository adminRepository) {
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // First check if user is a student
        Student student = studentRepository.findByUsername(username);
        if (student != null) {
            return new User(student.getUsername(),
                          student.getPassword(),
                          Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + student.getRole().name())));
        }

        // Then check if user is an admin
        Admin admin = adminRepository.findByUsername(username);
        if (admin != null) {
            return new User(admin.getUsername(),
                          admin.getPassword(),
                          Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name())));
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
