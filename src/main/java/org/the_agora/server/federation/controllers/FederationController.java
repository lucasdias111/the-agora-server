package org.the_agora.server.federation.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.the_agora.server.chat_messages.models.ChatMessage;
import org.the_agora.server.config.FederationConfig;
import org.the_agora.server.federation.models.FederatedMessageDTO;
import org.the_agora.server.federation.models.FederationInfo;
import org.the_agora.server.websocket.services.WebSocketClientService;

import java.util.Map;

@Slf4j
@RestController
public class FederationController {

    private final WebSocketClientService webSocketClientService;
    private final FederationConfig federationConfig;

    public FederationController(WebSocketClientService webSocketClientService, FederationConfig federationConfig) {
        this.webSocketClientService = webSocketClientService;
        this.federationConfig = federationConfig;
    }

    /**
     * Receive messages from other servers
     */
    @PostMapping("/federation/messages")
    public ResponseEntity<Void> receiveMessage(
            @RequestBody FederatedMessageDTO messageDTO,
            @RequestHeader("X-Server-Domain") String originServer) {

        log.info("Received federated message from {} to user {}",
                originServer, messageDTO.getToUserId());

        // TODO: Add server authentication/verification here

        ChatMessage message = new ChatMessage(
                messageDTO.getFromUserId(),
                messageDTO.getFromServer(),
                messageDTO.getToUserId(),
                federationConfig.getServerDomain(),
                messageDTO.getMessage()
        );

        webSocketClientService.sendMessageToUser(message);

        return ResponseEntity.ok().build();
    }

    /**
     * Well-known endpoint for server discovery
     */
    @GetMapping("/.well-known/federation")
    public FederationInfo getFederationInfo() {
        return FederationInfo.builder()
                .serverDomain(federationConfig.getServerDomain())
                .version("1.0")
                .endpoints(Map.of(
                        "messages", federationConfig.getPublicEndpoint() + "/federation/messages"
                ))
                .build();
    }
}