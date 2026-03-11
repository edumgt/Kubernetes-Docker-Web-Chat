package org.example.chatservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRealtimeEvent {
    private EventType type;
    private String conversationId;
    private String sender;
    private String recipient;
    private String participantOne;
    private String participantTwo;
    private Message message;
    private ReadReceiptEvent readReceipt;

    public enum EventType {
        PUBLIC_MESSAGE,
        PRIVATE_MESSAGE,
        READ_RECEIPT
    }
}

