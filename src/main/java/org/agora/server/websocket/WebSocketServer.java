package org.agora.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.agora.server.authentication.services.JwtService;
import org.agora.server.websocket.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private static final Map<String, Set<Channel>> userConnections = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final JwtService jwtService;

    @Value("${websocket.port:8081}")
    private int port;

    public WebSocketServer(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWebSocketServer() {
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(
                                        new HttpServerCodec(),
                                        new HttpObjectAggregator(65536),
                                        new WebSocketHandler(jwtService)
                                );
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                serverChannel = b.bind(port).sync().channel();
                log.info("WebSocket server started on port {}", port);
                serverChannel.closeFuture().sync();
            } catch (Exception e) {
                log.error("Failed to start WebSocket server", e);
            } finally {
                shutdown();
            }
        }, "netty-websocket-server").start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket server...");
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        userConnections.clear();
        log.info("WebSocket server shut down");
    }

    static class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
        private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);
        private WebSocketServerHandshaker handshaker;
        private String userId;
        private final JwtService jwtService;

        public WebSocketHandler(JwtService jwtService) {
            this.jwtService = jwtService;
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
                        userConnections.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                                .add(ctx.channel());
                        log.info("User {} connected", userId);
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
                Long userIdLong = jwtService.extractAllClaims(token).get("userId", Long.class);
                this.userId = username;

                log.info("WebSocket authenticated user: {} (id: {})", username, userIdLong);

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
                    JsonNode message = objectMapper.readTree(payload);
                    String type = message.get("type").asText();

                    if ("SEND_MESSAGE".equals(type)) {
                        String toUserId = message.get("toUserId").asText();
                        String messageText = message.get("message").asText();

                        sendMessageToUser(new Message(userId, toUserId, messageText));
                    }

                } catch (Exception e) {
                    log.error("Error handling message: {}", e.getMessage());
                }
            }
        }

        private void sendMessageToUser(Message outboundMessage) {
            Set<Channel> targetChannels = userConnections.get(outboundMessage.getToUserId());

            if (targetChannels == null || targetChannels.isEmpty()) {
                log.info("{} is not connected", outboundMessage.getToUserId());
                return;
            }

            try {
                String messageJson = outboundMessage.sendMessageToString();
                TextWebSocketFrame frame = new TextWebSocketFrame(messageJson);

                targetChannels.forEach(channel -> {
                    if (channel.isActive()) {
                        channel.writeAndFlush(frame.copy());
                    } else {
                        log.debug("Channel inactive for user {}",
                                outboundMessage.getToUserId());
                    }
                });

                frame.release();
            } catch (Exception e) {
                log.error("Error sending message: {}", e.getMessage());
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (userId != null) {
                removeUserConnection(userId, ctx.channel());
                log.info("User {} disconnected", userId);
            }
        }

        private void removeUserConnection(String userId, Channel channel) {
            Set<Channel> channels = userConnections.get(userId);
            if (channels != null) {
                channels.remove(channel);
                if (channels.isEmpty()) {
                    userConnections.remove(userId);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("WebSocket error for user {}: {}", userId, cause.getMessage());
            ctx.close();
        }
    }
}