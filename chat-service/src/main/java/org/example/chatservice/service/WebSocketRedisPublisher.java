package org.example.chatservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chatservice.model.ChatRealtimeEvent;
import org.example.chatservice.model.Message;
import org.example.chatservice.model.ReadReceiptEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.websocket.redis", name = "enabled", havingValue = "true")
public class WebSocketRedisPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String channel;

    public WebSocketRedisPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.websocket.redis.channel:chat:websocket}") String channel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channel = channel;
    }

    public void publishPublicMessage(Message message) {
        ChatRealtimeEvent event = new ChatRealtimeEvent(
                ChatRealtimeEvent.EventType.PUBLIC_MESSAGE,
                "public",
                message.getSender(),
                null,
                null,
                null,
                message,
                null
        );
        publish(event);
    }

    public void publishPrivateMessage(String conversationId, String sender, String recipient, Message message) {
        ChatRealtimeEvent event = new ChatRealtimeEvent(
                ChatRealtimeEvent.EventType.PRIVATE_MESSAGE,
                conversationId,
                sender,
                recipient,
                sender,
                recipient,
                message,
                null
        );
        publish(event);
    }

    public void publishReadReceipt(ReadReceiptEvent readReceiptEvent, String participantOne, String participantTwo) {
        ChatRealtimeEvent event = new ChatRealtimeEvent(
                ChatRealtimeEvent.EventType.READ_RECEIPT,
                readReceiptEvent.getConversationId(),
                readReceiptEvent.getReader(),
                null,
                participantOne,
                participantTwo,
                null,
                readReceiptEvent
        );
        publish(event);
    }

    private void publish(ChatRealtimeEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to publish Redis WebSocket event", e);
        }
    }
}

