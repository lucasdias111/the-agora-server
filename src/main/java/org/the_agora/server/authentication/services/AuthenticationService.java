package org.the_agora.server.authentication.services;

import org.the_agora.server.authentication.models.AuthResponse;
import org.the_agora.server.users.models.User;
import org.the_agora.server.users.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthenticationService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse authenticateUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOptional.get();

        if (!password.equals(user.getPassword())) { // TODO: Change to hashed passwords
            log.warn("Failed login attempt for user {}", username);
            throw new RuntimeException("Invalid credentials");
        }

        User updatedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                updatedUser.getUsername(),
                updatedUser.getId()
        );

        log.info("User {} logged in successfully", username);
        return new AuthResponse(token, "Login successful");
    }

    public User registerUser(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setCreatedAt(LocalDateTime.now());

        return userRepository.save(newUser);
    }
}