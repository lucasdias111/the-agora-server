package org.margin.server.social.services;

import org.margin.server.social.models.DirectMessage;
import org.margin.server.social.repositories.ChatMessageRepository;
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
