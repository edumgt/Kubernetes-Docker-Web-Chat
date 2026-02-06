package org.example.chatservice.model;


import lombok.Data;

@Data
public class PrivateMessageDTO {

    private String sender;
    private String recipient;
    private String content;

}
