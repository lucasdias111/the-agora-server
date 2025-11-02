package org.the_agora.server.authentication.services;

import org.the_agora.server.authentication.models.AuthResponse;
import org.the_agora.server.config.FederationConfig;
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
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_DURATION_SECONDS = 30;

	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final FederationConfig federationConfig;

	public AuthenticationService(UserRepository userRepository, JwtService jwtService, FederationConfig federationConfig) {
		this.userRepository = userRepository;
		this.jwtService = jwtService;
        this.federationConfig = federationConfig;
	}

	public AuthResponse authenticateUser(String username, String password) {
		Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            log.warn("Login attempt for non-existent user: {}", username);
            throw new RuntimeException("Login attempt for non-existent user: " + username);
        }

		User user = userOptional.get();

        if (isAccountLocked(user)) {
            log.warn("Login attempt for locked account: {}", username);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            handleFailedLogin(user);
            log.warn("Failed login attempt for user {} (attempt {}/{})",
                    username, user.getFailedLoginAttempts(), MAX_FAILED_ATTEMPTS);
        }

        resetFailedAttempts(user);

        String token = jwtService.generateToken(user.getUsername(), user.getId());

        log.info("User {} logged in successfully", username);
        return new AuthResponse(token, "Login successful");
	}

	public User registerUser(String username, String email, String password) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
        user.setServerDomain(federationConfig.getServerDomain());
		return userRepository.save(user);
	}

    private boolean isAccountLocked(User user) {
        if (user.getAccountLockedUntil() == null) {
            return false;
        }

        return user.getAccountLockedUntil().isAfter(LocalDateTime.now());
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        user.setLastFailedLoginAttempt(LocalDateTime.now());

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusSeconds(LOCK_DURATION_SECONDS));
            log.warn("Account locked for user {} until {}",
                    user.getUsername(), user.getAccountLockedUntil());
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLoginAttempt(null);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }
    }
}
