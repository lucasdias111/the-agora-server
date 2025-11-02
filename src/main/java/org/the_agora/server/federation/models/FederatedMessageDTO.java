package org.the_agora.server.federation.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FederatedMessageDTO {
    private Long fromUserId;
    private String fromServer;
    private Long toUserId;
    private String message;
    private Long timestamp;
}