package org.the_agora.server.websocket.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketMessage {
    private WebSocketMessageType type;
    private Long userId;
    private Long timestamp;
    private String payload;
}
