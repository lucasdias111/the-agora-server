package org.the_agora.server.websocket.factories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.the_agora.server.users.models.UserDTO;
import org.the_agora.server.websocket.models.WebSocketMessage;
import org.the_agora.server.websocket.models.WebSocketMessageType;

@Component
public class WebSocketMessageFactory {
    private final ObjectMapper mapper;

    public WebSocketMessageFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String createUserActivity(WebSocketMessageType type, UserDTO user) throws JsonProcessingException {
        WebSocketMessage message = new WebSocketMessage(
                type,
                user.getUsername(),
                System.currentTimeMillis(),
                null
        );
        return mapper.writeValueAsString(message);
    }

}
