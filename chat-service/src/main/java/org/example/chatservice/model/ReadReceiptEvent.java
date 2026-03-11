package org.example.chatservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptEvent {
    private String conversationId;
    private String reader;
    private Date readAt;
}

