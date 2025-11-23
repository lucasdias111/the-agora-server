package org.the_agora.server.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.the_agora.server.authentication.services.JwtService;
import org.the_agora.server.social.services.ChatMessageService;
import org.the_agora.server.config.FederationConfig;
import org.the_agora.server.users.UserService;
import org.the_agora.server.websocket.services.WebSocketClientService;

@Slf4j
@Component
public class WebSocketServer {

    @Value("${websocket.port:8081}")
    private int port;

    private final JwtService jwtService;
    private final WebSocketClientService clientService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final FederationConfig federationConfig;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public WebSocketServer(JwtService jwtService,
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

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread serverThread = new Thread(this::run, "websocket-server");
        serverThread.start();
    }

    private void run() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(createWebSocketHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverChannel = bootstrap.bind(port).sync().channel();
            log.info("WebSocket server started on port {}", port);

            serverChannel.closeFuture().sync();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("WebSocket server interrupted", e);
        } catch (Exception e) {
            log.error("Failed to start WebSocket server", e);
        } finally {
            shutdown();
        }
    }

    private WebSocketHandler createWebSocketHandler() {
        return new WebSocketHandler(jwtService, clientService, chatMessageService, userService, federationConfig);
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