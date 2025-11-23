package org.the_agora.server.social.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.the_agora.server.social.models.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<DirectMessage, Long> {
	@Query("""
			SELECT cm
			FROM DirectMessage cm
			WHERE cm.fromUserId = :fromUserId
			  AND cm.toUserId = :toUserId
			ORDER BY cm.createdAt ASC
			""")
	List<DirectMessage> findByFromUserIdAndToUserId(@Param("fromUserId") Long fromUserId,
                                                    @Param("toUserId") Long toUserId);
}
