package org.the_agora.server.websocket.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    public WebSocketClientService(WebSocketMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
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

    public void clearAllClients() {
        clients.clear();
    }
}
