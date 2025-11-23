package org.the_agora.server.websocket.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.the_agora.server.social.models.DirectMessage;
import org.the_agora.server.social.services.ChatMessageService;
import org.the_agora.server.config.FederationConfig;
import org.the_agora.server.federation.services.FederationService;
import org.the_agora.server.users.models.User;
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

	public void broadcastUserLogin(User loggedInUser) {
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

	public void broadcastUserLogout(User loggedOutUser) {
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

    public void sendMessageToUser(DirectMessage directMessage) {
        String recipientServer = parseServerFromUserId(directMessage.getToUserServer());

        if (isLocalUser(recipientServer)) {
            deliverLocalMessage(directMessage);
        } else {
            deliverFederatedMessage(directMessage, recipientServer);
        }
    }

	public void clearAllClients() {
		clients.clear();
	}


    private void deliverLocalMessage(DirectMessage directMessage) {
        Channel targetChannel = getClientChannel(directMessage.getToUserId());

        if (targetChannel == null) {
            log.debug("User {} is not connected, storing message for later", directMessage.getToUserId());
            // TODO: Send push notification for offline user
            chatMessageService.saveMessage(directMessage);
            return;
        }

        try {
            String messageJson = messageFactory.createWebSocketChatMessage(directMessage);
            TextWebSocketFrame frame = new TextWebSocketFrame(messageJson);

            if (targetChannel.isActive()) {
                targetChannel.writeAndFlush(frame.copy());
                chatMessageService.saveMessage(directMessage);
            } else {
                log.debug("Channel inactive for user {}", directMessage.getToUserId());
                // TODO: Send push notification for offline user
                chatMessageService.saveMessage(directMessage);
            }

            frame.release();
        } catch (JsonProcessingException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    private void deliverFederatedMessage(DirectMessage directMessage, String targetServer) {
        log.info("Routing message to federated server: {}", targetServer);

        if (directMessage.getFromUserServer() == null) {
            directMessage.setFromUserServer(federationConfig.getServerDomain());
        }
        directMessage.setToUserServer(targetServer);

        chatMessageService.saveMessage(directMessage);

        boolean success = federationService.sendFederatedMessage(directMessage);
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
