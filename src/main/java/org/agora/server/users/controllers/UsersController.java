package org.agora.server.users.controllers;

import org.agora.server.users.repositories.UserRepository;
import org.agora.server.websocket.services.WebSocketClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final WebSocketClientService webSocketClientService;
    private final UserRepository userRepository;

    public UsersController(WebSocketClientService webSocketClientService, UserRepository userRepository) {
        this.webSocketClientService = webSocketClientService;
        this.userRepository = userRepository;
    }

    @GetMapping("get_all_users")
    public List<String> getAllOnlineUsersOnServer() {
        return webSocketClientService.getAllClients().keySet()
                .stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(user -> user.get().getUsername())
                .toList();
    }
}
