package org.the_agora.server.users.controllers;

import org.springframework.security.core.Authentication;
import org.the_agora.server.users.UserService;
import org.the_agora.server.users.models.UserDTO;
import org.the_agora.server.websocket.services.WebSocketClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

	private final WebSocketClientService webSocketClientService;
	private final UserService userService;

	public UserController(WebSocketClientService webSocketClientService,
                          UserService userService) {
		this.webSocketClientService = webSocketClientService;
		this.userService = userService;
	}

	@GetMapping("get_all_users")
	public List<UserDTO> getAllOnlineUsersOnServer() {
		return webSocketClientService.getAllClients().keySet().stream().map(userService::getUserById).toList();
	}

    @GetMapping("/me")
    public UserDTO getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }
}
