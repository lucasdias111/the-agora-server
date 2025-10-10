package org.agora.server.websocket.services;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebSocketClientService {
    private final Map<Long, Channel> clients = new ConcurrentHashMap<>();

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
}
