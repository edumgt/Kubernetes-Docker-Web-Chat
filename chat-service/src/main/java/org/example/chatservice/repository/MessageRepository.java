package org.example.chatservice.repository;


import org.example.chatservice.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderByTimestampAsc(String conversationId);
    long countByConversationIdAndSenderNot(String conversationId, String sender);
    long countByConversationIdAndSenderNotAndTimestampAfter(String conversationId, String sender, Date timestamp);

}
