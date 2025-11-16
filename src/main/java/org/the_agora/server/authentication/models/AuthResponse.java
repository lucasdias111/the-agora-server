package org.the_agora.server.authentication.models;

public record AuthResponse(
        boolean success,
        String message,
        String username,
        String token
) {}
