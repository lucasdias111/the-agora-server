package org.the_agora.server.chat_messages.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long fromUserId;

    @NotNull
    @Column(nullable = false)
    private Long toUserId;

    @Column(name = "from_user_server")
    private String fromUserServer;

    @Column(name = "to_user_server")
    private String toUserServer;

    @NotBlank
    @Size(min = 1, max = 5000)
    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isEdited = false;

    @Column(nullable = false)
    private Date createdAt;

    @Column(name = "federated_message_id")
    private String federatedMessageId;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ChatMessage(Long fromUserId, String fromUserServer,
                       Long toUserId, String toUserServer, String message) {
        this.fromUserId = fromUserId;
        this.fromUserServer = fromUserServer;
        this.toUserId = toUserId;
        this.toUserServer = toUserServer;
        this.message = message;
        this.createdAt = new Date();
        this.federatedMessageId = generateFederatedMessageId(fromUserId, fromUserServer);
    }

    @JsonIgnore
    public boolean isFederatedMessage() {
        return fromUserServer != null && !fromUserServer.isEmpty();
    }

    @JsonIgnore
    public String getFromUserFederatedId() {
        return fromUserServer != null ? fromUserId + "@" + fromUserServer : String.valueOf(fromUserId);
    }

    @JsonIgnore
    public String getToUserFederatedId() {
        return toUserServer != null ? toUserId + "@" + toUserServer : String.valueOf(toUserId);
    }

    public String sendMessageToString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        if (federatedMessageId == null && fromUserServer != null) {
            federatedMessageId = generateFederatedMessageId(fromUserId, fromUserServer);
        }
    }

    private String generateFederatedMessageId(Long userId, String server) {
        return (server != null ? server : "local") + ":" + userId + ":" + System.currentTimeMillis();
    }
}