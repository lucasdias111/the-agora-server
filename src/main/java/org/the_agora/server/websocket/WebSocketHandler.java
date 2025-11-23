package org.the_agora.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.the_agora.server.authentication.services.JwtService;
import org.the_agora.server.social.models.DirectMessage;
import org.the_agora.server.social.services.ChatMessageService;
import org.the_agora.server.config.FederationConfig;
import org.the_agora.server.users.UserService;
import org.the_agora.server.users.models.User;
import org.the_agora.server.websocket.services.WebSocketClientService;

import java.util.Optional;

@Slf4j
@ChannelHandler.Sharable
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
    private final JwtService jwtService;
    private final WebSocketClientService clientService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final FederationConfig federationConfig;

    public WebSocketHandler(JwtService jwtService,
                            WebSocketClientService clientService,
                            ChatMessageService chatMessageService,
                            UserService userService,
                            FederationConfig federationConfig) {
        this.jwtService = jwtService;
        this.clientService = clientService;
        this.chatMessageService = chatMessageService;
        this.userService = userService;
        this.federationConfig = federationConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object message) {
        switch (message) {
            case FullHttpRequest httpRequest -> handleHttpRequest(ctx, httpRequest);
            case WebSocketFrame webSocketFrame -> handleWebSocketFrame(ctx, webSocketFrame);
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

        Optional<User> optionalUser = jwtService.extractAndValidateJwtTokenFromWebSocket(uri);

        if (optionalUser.isEmpty()) {
            ctx.close();
            return;
        }

        User user = optionalUser.get();
        ctx.channel().attr(WebSocketAttributes.USER).set(user);

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8081/ws", null, true, 65536);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            ctx.channel().attr(WebSocketAttributes.HANDSHAKER).set(handshaker);

            handshaker.handshake(ctx.channel(), req).addListener(future -> {
                if (future.isSuccess()) {
                    clientService.addClient(user.getId(), ctx.channel());
                    clientService.broadcastUserLogin(user);
                }
            });
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        User user = ctx.channel().attr(WebSocketAttributes.USER).get();
        WebSocketServerHandshaker handshaker = ctx.channel().attr(WebSocketAttributes.HANDSHAKER).get();

        switch (frame) {
            case CloseWebSocketFrame closeFrame -> {
                log.info("Client requested close: {}", user);
                handshaker.close(ctx.channel(), closeFrame);
            }
            case PingWebSocketFrame pingFrame -> {
                ctx.writeAndFlush(new PongWebSocketFrame(pingFrame.content().retain()));
            }
            case TextWebSocketFrame textFrame -> {
                handleTextWebSocketFrame(ctx, textFrame);
            }
            default -> throw new IllegalStateException("Unexpected value: " + frame);
        }
    }

    private void handleTextWebSocketFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        User user = ctx.channel().attr(WebSocketAttributes.USER).get();
        String payload = frame.text();
        log.debug("Received message from {}: {}", user, payload);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode message = mapper.readTree(payload);
            String type = message.get("type").asText();

            if ("SEND_MESSAGE".equals(type)) {
                String toUserIdIdentifier = message.get("toUserId").asText();
                String messageText = message.get("message").asText();

                DirectMessage directMessage = getChatMessage(user, toUserIdIdentifier, messageText);
                clientService.sendMessageToUser(directMessage);
            }

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    private DirectMessage getChatMessage(User user, String toUserIdIdentifier, String messageText) {
        String[] parts = toUserIdIdentifier.split("@", 2);
        User toUser = userService.getById(Long.parseLong(parts[0]));
        String toUserServer = parts.length > 1 ? parts[1] : null;

        return new DirectMessage(
                user.getId(),
                toUser.getId(),
                user.getServerDomain(),
                toUserServer,
                messageText
        );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        User user = ctx.channel().attr(WebSocketAttributes.USER).get();
        if (user != null) {
            clientService.removeClient(user.getId());
            clientService.broadcastUserLogout(user);
            log.info("User {} disconnected", user);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        User user = ctx.channel().attr(WebSocketAttributes.USER).get();
        log.error("WebSocket error for user {}: {}", user, cause.getMessage());
        ctx.close();
    }
}