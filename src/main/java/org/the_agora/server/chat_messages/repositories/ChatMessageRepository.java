package org.the_agora.server.chat_messages.repositories;

import org.the_agora.server.chat_messages.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByFromUserIdAndToUserIdOrderByCreatedAtAsc(Long userId, Long toUserId);
}
