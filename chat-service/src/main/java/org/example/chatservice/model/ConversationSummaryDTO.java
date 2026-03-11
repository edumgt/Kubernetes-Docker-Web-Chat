package org.example.chatservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDTO {
    private String conversationId;
    private String peerUser;
    private long unreadCount;
    private Date lastMessageAt;
    private Date lastReadAt;
    private String lastMessagePreview;
}

