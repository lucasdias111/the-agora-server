package org.the_agora.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.the_agora.server.authentication.services.JwtService;
import org.the_agora.server.chat_messages.services.ChatMessageService;
import org.the_agora.server.users.UserService;
import org.the_agora.server.websocket.services.WebSocketClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    static {
        new ObjectMapper();
    }

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final JwtService jwtService;
    private final WebSocketClientService clientService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    @Value("${websocket.port:8081}")
    private int port;

    public WebSocketServer(JwtService jwtService,
                           WebSocketClientService clientService,
                           ChatMessageService chatMessageService,
                           UserService userService) {
        this.jwtService = jwtService;
        this.clientService = clientService;
        this.chatMessageService = chatMessageService;
        this.userService = userService;
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
                                        new WebSocketHandler(jwtService, clientService, chatMessageService, userService)
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
        }, "the-agora-websocket-server").start();
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
        clientService.clearAllClients();
        log.info("WebSocket server shut down");
    }
}