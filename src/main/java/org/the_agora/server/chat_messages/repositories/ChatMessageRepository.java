package org.the_agora.server.chat_messages.repositories;

import org.the_agora.server.chat_messages.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByFromUserIdAndToUserIdOrToUserIdAndFromUserId(
            String fromUserId1, String toUserId1,
            String toUserId2, String fromUserId2);
    List<ChatMessage> findByToUserIdAndIsReadFalse(String toUserId);
    List<ChatMessage> findByFromUserIdOrderByCreatedAtDesc(String fromUserId);
    List<ChatMessage> findByToUserIdOrderByCreatedAtDesc(String toUserId);
    List<ChatMessage> findByFromUserIdAndToUserIdOrderByCreatedAtDesc(String fromUserId, String toUserId);
}
