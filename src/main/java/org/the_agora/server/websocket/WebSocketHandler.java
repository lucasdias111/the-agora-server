package org.the_agora.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.the_agora.server.authentication.services.JwtService;
import org.the_agora.server.chat_messages.models.ChatMessage;
import org.the_agora.server.chat_messages.services.ChatMessageService;
import org.the_agora.server.config.FederationConfig;
import org.the_agora.server.users.UserService;
import org.the_agora.server.users.models.UserDTO;
import org.the_agora.server.websocket.services.WebSocketClientService;

import java.net.URI;

@Slf4j
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
	private WebSocketServerHandshaker handshaker;
	private UserDTO user;
	private final JwtService jwtService;
	private final WebSocketClientService clientService;
	private final ChatMessageService chatMessageService;
	private final UserService userService;
    private final FederationConfig federationConfig;

	public WebSocketHandler(JwtService jwtService, WebSocketClientService clientService,
			ChatMessageService chatMessageService, UserService userService, FederationConfig federationConfig) {
		this.jwtService = jwtService;
		this.clientService = clientService;
		this.chatMessageService = chatMessageService;
		this.userService = userService;
        this.federationConfig = federationConfig;
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

		if (extractAndValidateJwtToken(ctx, uri))
			return;

		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
				"ws://localhost:" + 8081 + "/ws", null, true, 65536);
		handshaker = wsFactory.newHandshaker(req);

		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req).addListener(future -> {
				if (future.isSuccess()) {
					clientService.broadcastUserLogin(user);
					clientService.addClient(user.getId(), ctx.channel());
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
			this.user = userService.getUserById(jwtService.extractAllClaims(token).get("userId", Long.class));

			log.info("WebSocket authenticated user: {} (id: {})", username, user);

		} catch (Exception e) {
			log.error("JWT validation failed: {}", e.getMessage());
			ctx.close();
			return true;
		}
		return false;
	}

	private String extractTokenFromQuery(String query) {
		if (query == null)
			return null;

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
        switch (frame) {
            case CloseWebSocketFrame closeWebSocketFrame -> {
                log.info("Client requested close: {}", user);
                handshaker.close(ctx.channel(), closeWebSocketFrame);
            }
            case PingWebSocketFrame pingWebSocketFrame -> {
                ctx.writeAndFlush(pingWebSocketFrame);
            }
            case TextWebSocketFrame textWebSocketFrame -> {
                handleTextWebSocketFrame(textWebSocketFrame);
            }
            default -> throw new IllegalStateException("Unexpected value: " + frame);
        }
	}

    private void handleTextWebSocketFrame(TextWebSocketFrame textWebSocketFrame) {
        String payload = textWebSocketFrame.text();
        log.debug("Received message from {}: {}", user, payload);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode message = mapper.readTree(payload);
            String type = message.get("type").asText();

            if ("SEND_MESSAGE".equals(type)) {
                String toUserIdIdentifier = message.get("toUserId").asText();
                String messageText = message.get("message").asText();

                ChatMessage chatMessage = getChatMessage(toUserIdIdentifier, messageText);

                clientService.sendMessageToUser(chatMessage);
            }

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    private ChatMessage getChatMessage(String toUserIdIdentifier, String messageText) {
        String[] parts = toUserIdIdentifier.split("@", 2);
        Long toUserId = Long.parseLong(parts[0]);
        String toUserServer = parts.length > 1 ? parts[1] : null;

        return new ChatMessage(
                user.getId(),
                user.getServerDomain(),
                toUserId,
                toUserServer,
                messageText
        );
    }

    @Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (user != null) {
			clientService.removeClient(user.getId());
			clientService.broadcastUserLogout(user);
			log.info("User {} disconnected", user);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("WebSocket error for user {}: {}", user, cause.getMessage());
		ctx.close();
	}
}
