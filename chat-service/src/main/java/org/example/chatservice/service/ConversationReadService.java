package org.example.chatservice.service;

import org.example.chatservice.model.Conversation;
import org.example.chatservice.model.ConversationSummaryDTO;
import org.example.chatservice.model.ReadReceiptEvent;
import org.example.chatservice.repository.ConversationRepository;
import org.example.chatservice.repository.MessageRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConversationReadService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationReadService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public List<ConversationSummaryDTO> getConversationSummaries(String username) {
        List<Conversation> conversations = conversationRepository.findByUser1OrUser2(
                username,
                username,
                Sort.by(Sort.Order.desc("lastMessageAt"), Sort.Order.desc("createdAt"))
        );

        List<ConversationSummaryDTO> summaries = new ArrayList<>();
        for (Conversation conversation : conversations) {
            String peerUser = username.equals(conversation.getUser1()) ? conversation.getUser2() : conversation.getUser1();
            Date lastReadAt = Optional.ofNullable(conversation.getLastReadAtByUser())
                    .map(map -> map.get(username))
                    .orElse(null);

            long unreadCount = lastReadAt == null
                    ? messageRepository.countByConversationIdAndSenderNot(conversation.getId(), username)
                    : messageRepository.countByConversationIdAndSenderNotAndTimestampAfter(conversation.getId(), username, lastReadAt);

            summaries.add(new ConversationSummaryDTO(
                    conversation.getId(),
                    peerUser,
                    unreadCount,
                    conversation.getLastMessageAt(),
                    lastReadAt,
                    conversation.getLastMessagePreview()
            ));
        }

        return summaries;
    }

    public MarkReadResult markConversationAsRead(String conversationId, String username) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        if (!username.equals(conversation.getUser1()) && !username.equals(conversation.getUser2())) {
            throw new IllegalArgumentException("User is not a participant of this conversation");
        }

        Date now = new Date();
        Map<String, Date> lastReadAtByUser = Optional.ofNullable(conversation.getLastReadAtByUser())
                .orElseGet(HashMap::new);
        lastReadAtByUser.put(username, now);
        conversation.setLastReadAtByUser(lastReadAtByUser);

        Conversation savedConversation = conversationRepository.save(conversation);
        ReadReceiptEvent receipt = new ReadReceiptEvent(savedConversation.getId(), username, now);
        return new MarkReadResult(savedConversation, receipt);
    }

    public record MarkReadResult(Conversation conversation, ReadReceiptEvent receipt) { }
}

