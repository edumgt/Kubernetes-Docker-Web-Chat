package org.example.chatservice.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "messages")
@Data
public class Message {

    @Id
    private String id;
    private String sender;
    private String content;
    private Date timestamp;
    private String conversationId;

    public Message(){

    }

    public Message(String sender, String content, Date timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }
}
