package com.seriousapp.serious.app.users;

import com.seriousapp.serious.app.users.student.Student;
import com.seriousapp.serious.app.users.student.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService(
            UserRepository userRepository, StudentRepository studentRepository,
            BCryptPasswordEncoder bCryptPasswordEncoder
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Return CustomUserDetails instead of the User entity directly
        return new CustomUserDetails(user);
    }

    public Student update(Student student) {
        return studentRepository.save(student);
    }


//    public AuthenticationResponse authenticate(String username, String password) {
//        var checkEmailExists = userRepository.findByEmail(username);
//        log.info("Checking if email exists: {}", checkEmailExists);
//        if (checkEmailExists.isPresent()) {
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            username,
//                            password
//                    )
//            );
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//
//            var findUser = userRepository.findByEmail(username)
//                    .orElseThrow();
//
//            var jwtToken = jwtService.generateToken(userDetails);
//            var refreshToken = jwtService.generateRefreshToken(userDetails);
//
//            revokeAllUserTokens(userDetails.getUsername());
//
//            saveUserToken(userDetails, jwtToken);
//
//            return AuthenticationResponse.builder()
//                    .accessToken(jwtToken)
//                    .refreshToken(refreshToken)
//                    .build();
//        } else {
//            throw new IllegalStateException(String.format("%s not found!", username));
//        }
//    }

//    private void saveUserToken(UserPrincipal user, String jwtToken) {
//        var token = Token.builder()
//                .userPrincipalId(user.getId())
//                .token(jwtToken)
//                .tokenType(TokenType.BEARER)
//                .expired(false)
//                .revoked(false)
//                .build();
//        tokenRepository.save(token);
//    }

//    private void revokeAllUserTokens(String username) {
//        var validUserTokens = tokenRepository.findAllValidTokensByUserId(username);
//        if (validUserTokens.isEmpty())
//            return;
//        validUserTokens.forEach(token -> {
//            token.setExpired(true);
//            token.setRevoked(true);
//        });
//        tokenRepository.saveAll(validUserTokens);
//    }

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

//    public void refreshToken(
//            HttpServletRequest request,
//            HttpServletResponse response
//    ) throws IOException {
//        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
//        final String refreshToken;
//        final String userEmail;
//        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
//            return;
//        }
//        refreshToken = authHeader.substring(7);
//        userEmail = jwtService.extractUsername(refreshToken);
//        if (userEmail != null) {
//            var user = this.userRepository.findByEmail(userEmail)
//                    .orElseThrow();
//            if (jwtService.isTokenValid(refreshToken, user)) {
//                var accessToken = jwtService.generateToken(user);
//                revokeAllUserTokens(user);
//                saveUserToken(user, accessToken);
//                var authResponse = AuthenticationResponse.builder()
//                        .accessToken(accessToken)
//                        .refreshToken(refreshToken)
//                        .build();
//                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
//            }
//        }
//
//    }

    //create method that uploads a display picture to azure blob storage
//    public User uploadDisplayPicture(User user) {
//        var findUser = userRepository.findById(user.getId())
//                .orElseThrow(()-> new IllegalStateException("educator with id" + user.getId() + "does not exist"));
//        findUser.setDisplayPictureURL(user.getDisplayPictureURL());
//        return userRepository.save(findUser);
//    }
//
//    public User updateNames(User user, Long userId) {
//        var findUser = userRepository.findById(userId)
//                .orElseThrow(()-> new IllegalStateException("userPrincipal with id" + user.getId() + "does not exist"));
//        if (findUser.getRole().toString().equals("EDUCATOR")) {
//            findUser.setDescription(user.getDescription());
//            findUser.setFirstName(user.getFirstName());
//            findUser.setLastName(user.getLastName());
//            return userRepository.save(findUser);
//        }
//
//        findUser.setFirstName(user.getFirstName());
//        findUser.setLastName(user.getLastName());
//        return userRepository.save(findUser);
//    }

    // create a method to update userPrincipal password
public User updatePassword(User user, Long userId) {
        var findUser = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalStateException("userPrincipal with id" + user.getId() + "does not exist"));
        var encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        findUser.setPassword(encodedPassword);
        return userRepository.save(findUser);
    }


}
