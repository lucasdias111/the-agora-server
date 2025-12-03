package org.margin.server.authentication.models;

public record AuthResponse(
        boolean success,
        String message,
        String username,
        String token
) {}
