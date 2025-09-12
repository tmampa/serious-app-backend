package com.seriousapp.serious.app.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seriousapp.serious.app.jwt.JwtService;
import com.seriousapp.serious.app.jwt.token.AuthenticationResponse;
import com.seriousapp.serious.app.jwt.token.Token;
import com.seriousapp.serious.app.jwt.token.TokenRepository;
import com.seriousapp.serious.app.jwt.token.TokenType;
import com.seriousapp.serious.app.users.student.Student;
import com.seriousapp.serious.app.users.student.StudentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UserService implements UserDetailsService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    private final StudentRepository studentRepository;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService(
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            TokenRepository tokenRepository,
            UserRepository userRepository, StudentRepository studentRepository,
            BCryptPasswordEncoder bCryptPasswordEncoder
    ) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("%s not found", username)));
    }

    public Student update(Student student) {
        return studentRepository.save(student);
    }


    public AuthenticationResponse authenticate(User user) {

        var checkEmailExists = userRepository.findByEmail(user.getUsername()).isPresent();
        if (checkEmailExists) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword()
                    )
            );
            var findUser = userRepository.findByEmail(user.getUsername())
                    .orElseThrow();
            var jwtToken = jwtService.generateToken(findUser);
            var refreshToken = jwtService.generateRefreshToken(findUser);
            revokeAllUserTokens(findUser);
            saveUserToken(findUser, jwtToken);

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .role(findUser.getRole())
                    .build();
        } else {
            throw new IllegalStateException(String.format("%s not found!", user.getUsername()));
        }
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokensByUserId(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public User getAuthenticatedUser() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            String email = ((User) principal).getUsername();
            return userRepository.findByEmail(email).orElseThrow();
        }
        return null;
    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.userRepository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }

    }

    //create method that uploads a display picture to azure blob storage
    public User uploadDisplayPicture(User user) {
        var findUser = userRepository.findById(user.getId())
                .orElseThrow(()-> new IllegalStateException("educator with id" + user.getId() + "does not exist"));
        findUser.setDisplayPictureURL(user.getDisplayPictureURL());
        return userRepository.save(findUser);
    }

    public User updateNames(User user, Long userId) {
        var findUser = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalStateException("user with id" + user.getId() + "does not exist"));
        if (findUser.getRole().toString().equals("EDUCATOR")) {
            findUser.setDescription(user.getDescription());
            findUser.setFirstName(user.getFirstName());
            findUser.setLastName(user.getLastName());
            return userRepository.save(findUser);
        }

        findUser.setFirstName(user.getFirstName());
        findUser.setLastName(user.getLastName());
        return userRepository.save(findUser);
    }

    // create a method to update user password
public User updatePassword(User user, Long userId) {
        var findUser = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalStateException("user with id" + user.getId() + "does not exist"));
        var encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        findUser.setPassword(encodedPassword);
        return userRepository.save(findUser);
    }


}
