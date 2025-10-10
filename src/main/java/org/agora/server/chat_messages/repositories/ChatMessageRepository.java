package org.agora.server.chat_messages.repositories;

import org.agora.server.chat_messages.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByFromUserIdAndToUserIdOrToUserIdAndFromUserId(
            Long fromUserId1, Long toUserId1,
            Long toUserId2, Long fromUserId2);
    List<ChatMessage> findByToUserIdAndIsReadFalse(Long toUserId);
    List<ChatMessage> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId);
    List<ChatMessage> findByToUserIdOrderByCreatedAtDesc(Long toUserId);
    List<ChatMessage> findByFromUserIdAndToUserIdOrderByCreatedAtDesc(Long fromUserId, Long toUserId);
}
