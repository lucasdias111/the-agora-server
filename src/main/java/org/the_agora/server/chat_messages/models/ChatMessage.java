package org.the_agora.server.chat_messages.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    @Column(nullable = false)
    public String fromUserId;

    @NotBlank
    @Column(nullable = false)
    public String toUserId;

    @NotBlank
    @Size(min = 1, max = 5000)
    @Column(nullable = false)
    public String message;

    public Boolean isRead = false;

    public Boolean isEdited = false;

    public Date createdAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ChatMessage(String userId, String toUserId, String messageText) {
        this.fromUserId = userId;
        this.toUserId = toUserId;
        this.message = messageText;
    }

    public String sendMessageToString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }
}