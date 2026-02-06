package org.example.chatservice.repository;

import org.example.chatservice.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByUser1AndUser2(String user1, String user2);

}
