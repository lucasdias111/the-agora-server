package org.the_agora.server.social.services;

import org.the_agora.server.social.models.DirectMessage;
import org.the_agora.server.social.repositories.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class MessageService {
	private final ChatMessageRepository messageRepository;

	public MessageService(ChatMessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}

	public void saveMessage(DirectMessage message) {
		message.setCreatedAt(new Date());
		messageRepository.save(message);
	}
}
