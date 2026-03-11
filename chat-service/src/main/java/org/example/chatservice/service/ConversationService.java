package org.example.chatservice.service;

import org.example.chatservice.model.Conversation;
import org.example.chatservice.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;

@Service
public class ConversationService {


    private final ConversationRepository conversationRepository;


    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation getOrCreateConversation(String user1, String user2){

        String user1_ = user1.compareTo(user2) <= 0 ? user1: user2;
        String user2_ = user1.compareTo(user2) <= 0 ? user2: user1;

        String conversationId = user1_ + "_" + user2_;
        return conversationRepository.findById(conversationId)
                .orElseGet(
                        ()->{
                            Conversation conversation = new Conversation();
                            conversation.setId(conversationId);
                            conversation.setUser1(user1_);
                            conversation.setUser2(user2_);
                            conversation.setCreatedAt(new Date());
                            conversation.setLastReadAtByUser(new HashMap<>());
                            return conversationRepository.save(conversation);
                        });

    }
}
