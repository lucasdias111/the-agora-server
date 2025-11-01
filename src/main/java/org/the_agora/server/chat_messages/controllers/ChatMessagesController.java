package org.the_agora.server.chat_messages.controllers;

import org.the_agora.server.chat_messages.models.ChatMessage;
import org.the_agora.server.chat_messages.repositories.ChatMessageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/chat_messages")
public class ChatMessagesController {
	private final ChatMessageRepository chatMessageRepository;

	public ChatMessagesController(ChatMessageRepository chatMessageRepository) {
		this.chatMessageRepository = chatMessageRepository;
	}

	@GetMapping("get_chat_history")
	public List<ChatMessage> getChatHistory(@RequestParam Long userId, @RequestParam Long toUserId) {
		List<ChatMessage> messagesFromUser = chatMessageRepository.findByFromUserIdAndToUserId(userId, toUserId);
		List<ChatMessage> messagesToUser = chatMessageRepository.findByFromUserIdAndToUserId(toUserId, userId);

		List<ChatMessage> allMessages = new ArrayList<>();
		allMessages.addAll(messagesFromUser);
		allMessages.addAll(messagesToUser);

		allMessages.sort(Comparator.comparing(ChatMessage::getCreatedAt));

		return allMessages;
	}
}
