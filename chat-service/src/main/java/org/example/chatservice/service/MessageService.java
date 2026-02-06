package org.example.chatservice.service;


import org.example.chatservice.model.Message;
import org.example.chatservice.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;


    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message saveMessage(String conversationId , String sender, String content){
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSender(sender);
        message.setContent(content);
        message.setTimestamp(new Date());
        return messageRepository.save(message);
    }

    public List<Message> getMessagesForConversation(String conversationId){
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

}
