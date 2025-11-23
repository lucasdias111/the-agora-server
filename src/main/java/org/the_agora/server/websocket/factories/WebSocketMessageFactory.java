package org.the_agora.server.websocket.factories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.the_agora.server.social.models.DirectMessage;
import org.the_agora.server.users.UserService;
import org.the_agora.server.users.models.User;
import org.the_agora.server.websocket.models.WebSocketMessage;
import org.the_agora.server.websocket.models.WebSocketMessageType;

@Component
public class WebSocketMessageFactory {
	private final ObjectMapper mapper;
	private final UserService userService;

	public WebSocketMessageFactory(ObjectMapper mapper, UserService userService) {
		this.mapper = mapper;
		this.userService = userService;
	}

	public String createUserActivity(WebSocketMessageType type, User user) throws JsonProcessingException {
		WebSocketMessage message = new WebSocketMessage(type, mapper.writeValueAsString(user),
				System.currentTimeMillis(), null);
		return mapper.writeValueAsString(message);
	}

	public String createWebSocketChatMessage(DirectMessage directMessage) throws JsonProcessingException {
		WebSocketMessage message = new WebSocketMessage(WebSocketMessageType.SEND_MESSAGE,
				mapper.writeValueAsString(userService.getById(directMessage.getToUserId())), System.currentTimeMillis(),
				mapper.writeValueAsString(directMessage));
		return mapper.writeValueAsString(message);
	}
}
