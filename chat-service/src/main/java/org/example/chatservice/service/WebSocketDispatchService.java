package org.example.chatservice.service;

import org.example.chatservice.model.Message;
import org.example.chatservice.model.ReadReceiptEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class WebSocketDispatchService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final WebSocketRedisPublisher webSocketRedisPublisher;

    public WebSocketDispatchService(
            SimpMessagingTemplate simpMessagingTemplate,
            ObjectProvider<WebSocketRedisPublisher> webSocketRedisPublisherProvider
    ) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.webSocketRedisPublisher = webSocketRedisPublisherProvider.getIfAvailable();
    }

    public void dispatchPublicMessage(Message message) {
        if (webSocketRedisPublisher != null) {
            webSocketRedisPublisher.publishPublicMessage(message);
            return;
        }
        forwardPublicMessage(message);
    }

    public void dispatchPrivateMessage(String conversationId, String sender, String recipient, Message message) {
        if (webSocketRedisPublisher != null) {
            webSocketRedisPublisher.publishPrivateMessage(conversationId, sender, recipient, message);
            return;
        }
        forwardPrivateMessage(conversationId, sender, recipient, message);
    }

    public void dispatchReadReceipt(ReadReceiptEvent readReceiptEvent, String participantOne, String participantTwo) {
        if (webSocketRedisPublisher != null) {
            webSocketRedisPublisher.publishReadReceipt(readReceiptEvent, participantOne, participantTwo);
            return;
        }
        forwardReadReceipt(readReceiptEvent, participantOne, participantTwo);
    }

    public void forwardPublicMessage(Message message) {
        simpMessagingTemplate.convertAndSend("/topic/messages", message);
    }

    public void forwardPrivateMessage(String conversationId, String sender, String recipient, Message message) {
        Set<String> targets = new LinkedHashSet<>();
        targets.add(sender);
        targets.add(recipient);

        for (String target : targets) {
            simpMessagingTemplate.convertAndSendToUser(target, "/queue/private/" + conversationId, message);
        }
    }

    public void forwardReadReceipt(ReadReceiptEvent readReceiptEvent, String participantOne, String participantTwo) {
        Set<String> targets = new LinkedHashSet<>();
        targets.add(participantOne);
        targets.add(participantTwo);

        for (String target : targets) {
            simpMessagingTemplate.convertAndSendToUser(
                    target,
                    "/queue/private/" + readReceiptEvent.getConversationId() + "/read",
                    readReceiptEvent
            );
        }
    }
}

