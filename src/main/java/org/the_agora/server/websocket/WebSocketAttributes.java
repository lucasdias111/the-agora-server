package org.the_agora.server.websocket;

import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.AttributeKey;
import org.the_agora.server.users.models.User;

public class WebSocketAttributes {
    public static final AttributeKey<User> USER = AttributeKey.valueOf("user");
    public static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER =
            AttributeKey.valueOf("handshaker");
}