package org.the_agora.server.users.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.the_agora.server.users.UserService;
import org.the_agora.server.users.models.User;
import org.the_agora.server.users.models.UserDTO;
import org.the_agora.server.users.models.keys.KeyUploadRequest;
import org.the_agora.server.users.models.keys.PrivateKeyResponse;
import org.the_agora.server.users.models.keys.PublicKeyResponse;
import org.the_agora.server.websocket.services.WebSocketClientService;

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
	public List<UserDTO> getAllOnlineUsersOnServer(Authentication authentication) {
        String username = authentication.getName();
		return webSocketClientService.getAllClients()
                .keySet()
                .stream()
                .map(userService::getById)
                .filter(user -> !user.getUsername().equals(username))
                .map(UserDTO::new)
                .toList();
	}

    @GetMapping("/me")
    public UserDTO getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return new UserDTO(userService.getByUsername(username));
    }

    @PostMapping("/{userId}/keys")
    public ResponseEntity<Void> uploadKeys(
            @PathVariable Long userId,
            @RequestBody KeyUploadRequest request,
            @AuthenticationPrincipal User authenticatedUser) {
        if (!authenticatedUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userService.savePublicPrivateKeysForUser(userId, request.getPublicKey(), request.getEncryptedPrivateKey());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/public-key")
    public PublicKeyResponse getPublicKey(@PathVariable Long userId) {
        User user = userService.getById(userId);
        return new PublicKeyResponse(user.getId(), user.getPublicKey());
    }

    @GetMapping("/me/private-key")
    public ResponseEntity<PrivateKeyResponse> getEncryptedPrivateKey(
            @AuthenticationPrincipal User user) {

        if (user.getEncryptedPrivateKey() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new PrivateKeyResponse(user.getEncryptedPrivateKey()));
    }
}
