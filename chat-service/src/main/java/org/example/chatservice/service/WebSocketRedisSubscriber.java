package org.example.chatservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.chatservice.model.ChatRealtimeEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.websocket.redis", name = "enabled", havingValue = "true")
public class WebSocketRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final WebSocketDispatchService webSocketDispatchService;

    public WebSocketRedisSubscriber(ObjectMapper objectMapper, WebSocketDispatchService webSocketDispatchService) {
        this.objectMapper = objectMapper;
        this.webSocketDispatchService = webSocketDispatchService;
    }

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatRealtimeEvent event = objectMapper.readValue(payload, ChatRealtimeEvent.class);

            if (event.getType() == null) {
                return;
            }

            switch (event.getType()) {
                case PUBLIC_MESSAGE -> webSocketDispatchService.forwardPublicMessage(event.getMessage());
                case PRIVATE_MESSAGE -> webSocketDispatchService.forwardPrivateMessage(
                        event.getConversationId(),
                        event.getSender(),
                        event.getRecipient(),
                        event.getMessage()
                );
                case READ_RECEIPT -> webSocketDispatchService.forwardReadReceipt(
                        event.getReadReceipt(),
                        event.getParticipantOne(),
                        event.getParticipantTwo()
                );
            }
        } catch (Exception e) {
            log.error("Failed to consume Redis WebSocket event", e);
        }
    }
}

