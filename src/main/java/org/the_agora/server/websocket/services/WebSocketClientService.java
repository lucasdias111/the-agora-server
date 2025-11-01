package org.the_agora.server.websocket.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.the_agora.server.chat_messages.models.ChatMessage;
import org.the_agora.server.chat_messages.services.ChatMessageService;
import org.the_agora.server.users.models.UserDTO;
import org.the_agora.server.websocket.factories.WebSocketMessageFactory;
import org.the_agora.server.websocket.models.WebSocketMessageType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebSocketClientService {
	private final Map<Long, Channel> clients = new ConcurrentHashMap<>();
	private final WebSocketMessageFactory messageFactory;
	private final ChatMessageService chatMessageService;

	public WebSocketClientService(WebSocketMessageFactory messageFactory, ChatMessageService chatMessageService) {
		this.messageFactory = messageFactory;
		this.chatMessageService = chatMessageService;
	}

	public void addClient(Long id, Channel channel) {
		clients.put(id, channel);
		log.info("User {} has connected", id);
	}

	public void removeClient(Long id) {
		clients.remove(id);
		log.info("User {} has disconnected", id);
	}

	public Map<Long, Channel> getAllClients() {
		return clients;
	}

	public Channel getClientChannel(Long toUserId) {
		return clients.get(toUserId);
	}

	public void broadcastUserLogin(UserDTO loggedInUser) {
		try {
			String messageJson = messageFactory.createUserActivity(WebSocketMessageType.USER_LOGIN, loggedInUser);
			TextWebSocketFrame frame = new TextWebSocketFrame(messageJson);

			for (Channel channel : clients.values()) {
				if (channel.isActive()) {
					channel.writeAndFlush(frame.copy());
				}
			}

			frame.release();
		} catch (Exception e) {
			log.error("Error broadcasting user login: {}", e.getMessage());
		}
	}

	public void broadcastUserLogout(UserDTO loggedOutUser) {
		try {
			String messageJson = messageFactory.createUserActivity(WebSocketMessageType.USER_LOGOUT, loggedOutUser);
			TextWebSocketFrame frame = new TextWebSocketFrame(messageJson);

			for (Channel channel : clients.values()) {
				if (channel.isActive()) {
					channel.writeAndFlush(frame.copy());
				}
			}

			frame.release();
		} catch (Exception e) {
			log.error("Error broadcasting user logout: {}", e.getMessage());
		}
	}

	public void sendMessageToUser(ChatMessage chatMessage) {

		Channel targetChannel = getClientChannel(chatMessage.getToUserId());

		if (targetChannel == null) {
			log.info("{} is not connected", chatMessage.getToUserId());
			return;
		}

		try {
			String messageJson = messageFactory.createWebSocketChatMessage(chatMessage);

			TextWebSocketFrame frame = new TextWebSocketFrame(messageJson);

			if (targetChannel.isActive()) {
				targetChannel.writeAndFlush(frame.copy());
				chatMessageService.saveMessage(chatMessage);
			} else {
				log.debug("Channel inactive for user {}", chatMessage.getToUserId());
			}

			frame.release();
		} catch (JsonProcessingException e) {
			log.error("Error sending message: {}", e.getMessage());
		}
	}

	public void clearAllClients() {
		clients.clear();
	}
}
