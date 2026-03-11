package org.example.chatservice.repository;

import org.example.chatservice.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByUser1AndUser2(String user1, String user2);
    List<Conversation> findByUser1OrUser2(String user1, String user2, Sort sort);

}
