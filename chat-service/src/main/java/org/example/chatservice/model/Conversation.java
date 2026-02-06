package org.example.chatservice.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@ToString
@Data
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;
    private String user1;
    private String user2;
    private Date createdAt;


}
