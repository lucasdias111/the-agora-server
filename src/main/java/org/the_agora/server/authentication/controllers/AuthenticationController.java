package org.the_agora.server.authentication.controllers;

import lombok.extern.slf4j.Slf4j;
import org.the_agora.server.authentication.models.AuthResponse;
import org.the_agora.server.authentication.services.AuthenticationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthenticationController {
    private final AuthenticationService authService;

    public AuthenticationController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        log.info("Login attempt for username: {}", request.username());

        return authService.authenticateUser(request.username(), request.password());
    }
}

record LoginRequest(String username, String password) {}