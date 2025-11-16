package org.the_agora.server.websocket.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.the_agora.server.chat.models.ChatMessage;
import org.the_agora.server.chat.services.ChatMessageService;
import org.the_agora.server.config.FederationConfig;
import org.the_agora.server.federation.services.FederationService;
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
    private final FederationConfig federationConfig;
    private final FederationService federationService;

	public WebSocketClientService(WebSocketMessageFactory messageFactory,
                                  ChatMessageService chatMessageService,
                                  FederationConfig federationConfig,
                                  FederationService federationService) {
		this.messageFactory = messageFactory;
		this.chatMessageService = chatMessageService;
        this.federationConfig = federationConfig;
        this.federationService = federationService;
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
        String recipientServer = parseServerFromUserId(chatMessage.getToUserServer());

        if (isLocalUser(recipientServer)) {
            deliverLocalMessage(chatMessage);
        } else {
            deliverFederatedMessage(chatMessage, recipientServer);
        }
    }

	public void clearAllClients() {
		clients.clear();
	}


    private void deliverLocalMessage(ChatMessage chatMessage) {
        Channel targetChannel = getClientChannel(chatMessage.getToUserId());

        if (targetChannel == null) {
            log.debug("User {} is not connected, storing message for later", chatMessage.getToUserId());
            // TODO: Send push notification for offline user
            chatMessageService.saveMessage(chatMessage);
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
                // TODO: Send push notification for offline user
                chatMessageService.saveMessage(chatMessage);
            }

            frame.release();
        } catch (JsonProcessingException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    private void deliverFederatedMessage(ChatMessage chatMessage, String targetServer) {
        log.info("Routing message to federated server: {}", targetServer);

        if (chatMessage.getFromUserServer() == null) {
            chatMessage.setFromUserServer(federationConfig.getServerDomain());
        }
        chatMessage.setToUserServer(targetServer);

        chatMessageService.saveMessage(chatMessage);

        boolean success = federationService.sendFederatedMessage(chatMessage);
        if (!success) {
            log.error("Failed to deliver federated message to {}", targetServer);
            // TODO: Mark message as failed, implement retry logic or callback the error to client
        }
    }

    private boolean isLocalUser(String serverDomain) {
        return serverDomain == null ||
                serverDomain.isEmpty() ||
                serverDomain.equals(federationConfig.getServerDomain());
    }

    private String parseServerFromUserId(String serverHint) {
        if (serverHint != null && !serverHint.isEmpty()) {
            return serverHint;
        }

        return federationConfig.getServerDomain();
    }
}
