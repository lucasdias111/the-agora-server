package org.the_agora.server.chat.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.the_agora.server.chat.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
	@Query("""
			SELECT cm
			FROM ChatMessage cm
			WHERE cm.fromUserId = :fromUserId
			  AND cm.toUserId = :toUserId
			ORDER BY cm.createdAt ASC
			""")
	List<ChatMessage> findByFromUserIdAndToUserId(@Param("fromUserId") Long fromUserId,
			@Param("toUserId") Long toUserId);
}
