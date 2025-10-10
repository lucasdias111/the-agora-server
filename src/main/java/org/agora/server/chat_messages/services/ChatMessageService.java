package org.agora.server.chat_messages.services;

import org.agora.server.chat_messages.models.ChatMessage;
import org.agora.server.chat_messages.repositories.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ChatMessageService {
    @Autowired
    private ChatMessageRepository messageRepository;

    public void saveMessage(ChatMessage message) {
        message.setCreatedAt(new Date());
        messageRepository.save(message);
    }
}
