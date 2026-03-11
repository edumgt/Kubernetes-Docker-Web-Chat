package org.example.chatservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrivateConversationDTO {
    private String conversationId;
    private String sender;
    private String recipient;
}

