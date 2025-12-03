package org.margin.server.social.controllers;

import org.margin.server.social.models.DirectMessage;
import org.margin.server.social.repositories.ChatMessageRepository;
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
	public List<DirectMessage> getChatHistory(@RequestParam Long userId, @RequestParam Long toUserId) {
		List<DirectMessage> messagesFromUser = chatMessageRepository.findByFromUserIdAndToUserId(userId, toUserId);
		List<DirectMessage> messagesToUser = chatMessageRepository.findByFromUserIdAndToUserId(toUserId, userId);

		List<DirectMessage> allMessages = new ArrayList<>();
		allMessages.addAll(messagesFromUser);
		allMessages.addAll(messagesToUser);

		allMessages.sort(Comparator.comparing(DirectMessage::getCreatedAt));

		return allMessages;
	}
}
