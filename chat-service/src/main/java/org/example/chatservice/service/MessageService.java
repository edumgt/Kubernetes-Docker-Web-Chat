package org.example.chatservice.service;


import org.example.chatservice.model.Message;
import org.example.chatservice.model.Conversation;
import org.example.chatservice.repository.ConversationRepository;
import org.example.chatservice.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;


    public MessageService(MessageRepository messageRepository, ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    public Message saveMessage(String conversationId , String sender, String content){
        return saveMessage(conversationId, sender, content, new Date());
    }

    public Message saveMessage(String conversationId , String sender, String content, Date timestamp){
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSender(sender);
        message.setContent(content);
        message.setTimestamp(timestamp);
        message.setReadBy(Collections.singleton(sender));

        Message saved = messageRepository.save(message);
        updateConversationSummary(conversationId, content, timestamp);
        return saved;
    }

    public List<Message> getMessagesForConversation(String conversationId){
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    private void updateConversationSummary(String conversationId, String content, Date timestamp) {
        if ("public".equals(conversationId)) {
            return;
        }

        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setLastMessageAt(timestamp);
            conversation.setLastMessagePreview(trimPreview(content));
            conversationRepository.save(conversation);
        });
    }

    private String trimPreview(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 120 ? content.substring(0, 120) + "..." : content;
    }

}
