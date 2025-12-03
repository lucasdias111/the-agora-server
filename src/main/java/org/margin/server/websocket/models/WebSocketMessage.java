package org.margin.server.websocket.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketMessage {
	private WebSocketMessageType type;
	private String userJson;
	private Long timestamp;
	private String payload;
}
