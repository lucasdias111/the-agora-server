package org.the_agora.server.users.controllers;

import org.springframework.web.bind.annotation.RequestHeader;
import org.the_agora.server.authentication.services.JwtService;
import org.the_agora.server.users.UserService;
import org.the_agora.server.users.models.UserDTO;
import org.the_agora.server.users.repositories.UserRepository;
import org.the_agora.server.websocket.services.WebSocketClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final WebSocketClientService webSocketClientService;
    private final UserService userService;
    private final JwtService jwtService;


    public UsersController(WebSocketClientService webSocketClientService, UserRepository userRepository, UserService userService, JwtService jwtService) {
        this.webSocketClientService = webSocketClientService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("get_all_users")
    public List<UserDTO> getAllOnlineUsersOnServer() {
        return webSocketClientService.getAllClients().keySet()
                .stream()
                .map(userService::getUserById)
                .toList();
    }

    @GetMapping("me")
    public UserDTO getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }

        String token = authHeader.substring(7);
        Long userId = jwtService.extractUserId(token);
        return userService.getUserById(userId);
    }
}
