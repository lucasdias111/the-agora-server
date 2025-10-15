package org.the_agora.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.the_agora.server.authentication.services.JwtService;
import org.the_agora.server.chat_messages.models.ChatMessage;
import org.the_agora.server.chat_messages.services.ChatMessageService;
import org.the_agora.server.websocket.services.WebSocketClientService;

import java.net.URI;

@Slf4j
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
    private WebSocketServerHandshaker handshaker;
    private Long userId;
    private final JwtService jwtService;
    private final WebSocketClientService clientService;
    private final ChatMessageService chatMessageService;

    public WebSocketHandler(JwtService jwtService,
                            WebSocketClientService clientService,
                            ChatMessageService chatMessageService) {
        this.jwtService = jwtService;
        this.clientService = clientService;
        this.chatMessageService = chatMessageService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, Object message) {
        switch (message) {
            case FullHttpRequest httpRequest -> handleHttpRequest(handlerContext, httpRequest);
            case WebSocketFrame webSocketFrame -> handleWebSocketFrame(handlerContext, webSocketFrame);
            default -> throw new IllegalStateException("Unexpected value: " + message);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            log.warn("Bad HTTP request from {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        String uri = req.uri();
        if (!uri.startsWith("/ws")) {
            log.warn("Invalid WebSocket path: {}", uri);
            ctx.close();
            return;
        }

        if (extractAndValidateJwtToken(ctx, uri)) return;

        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(
                        "ws://localhost:" + 8081 + "/ws",
                        null,
                        true,
                        65536
                );
        handshaker = wsFactory.newHandshaker(req);

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req).addListener(future -> {
                if (future.isSuccess()) {
                    clientService.broadcastUserLogin(userId);
                    clientService.addClient(userId, ctx.channel());
                }
            });
        }
    }

    private boolean extractAndValidateJwtToken(ChannelHandlerContext ctx, String uri) {
        try {
            URI fullUri = new URI(uri);
            String query = fullUri.getQuery();
            String token = extractTokenFromQuery(query);

            if (token == null || token.isEmpty()) {
                log.warn("Missing token in WebSocket connection");
                ctx.close();
                return true;
            }

            if (!jwtService.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token");
                ctx.close();
                return true;
            }

            String username = jwtService.extractUsername(token);
            this.userId = jwtService.extractAllClaims(token).get("userId", Long.class);

            log.info("WebSocket authenticated user: {} (id: {})", username, userId);

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            ctx.close();
            return true;
        }
        return false;
    }

    private String extractTokenFromQuery(String query) {
        if (query == null) return null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            log.info("Client requested close: {}", userId);
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            String payload = ((TextWebSocketFrame) frame).text();
            log.debug("Received message from {}: {}", userId, payload);

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode message = mapper.readTree(payload);
                String type = message.get("type").asText();

                if ("SEND_MESSAGE".equals(type)) {
                    Long toUserId = message.get("toUserId").asLong();
                    String messageText = message.get("message").asText();

                    sendMessageToUser(new ChatMessage(userId.toString(), toUserId.toString(), messageText));
                }

            } catch (Exception e) {
                log.error("Error handling message: {}", e.getMessage());
            }
        }
    }

    private void sendMessageToUser(ChatMessage outboundMessage) {
        Channel targetChannel = clientService.getClientChannel(Long.valueOf(outboundMessage.getToUserId()));

        if (targetChannel == null) {
            log.info("{} is not connected", outboundMessage.getToUserId());
            return;
        }

        try {
            String messageJson = outboundMessage.sendMessageToString();
            TextWebSocketFrame frame = new TextWebSocketFrame(messageJson);

            if (targetChannel.isActive()) {
                targetChannel.writeAndFlush(frame.copy());
                chatMessageService.saveMessage(outboundMessage);
            } else {
                log.debug("Channel inactive for user {}",
                        outboundMessage.getToUserId());
            }

            frame.release();
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (userId != null) {
            clientService.removeClient(userId);
            clientService.broadcastUserLogout(userId);
            log.info("User {} disconnected", userId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket error for user {}: {}", userId, cause.getMessage());
        ctx.close();
    }
}
