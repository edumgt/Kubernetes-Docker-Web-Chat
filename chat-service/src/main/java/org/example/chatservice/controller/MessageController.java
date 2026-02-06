package org.example.chatservice.controller;


import lombok.extern.slf4j.Slf4j;
import org.example.chatservice.model.Message;
import org.example.chatservice.model.User;
import org.example.chatservice.service.MessageService;
import org.example.chatservice.service.UserServiceClient;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
public class MessageController {

    private final MessageService messageService;
    private final UserServiceClient userServiceClient;



    public MessageController(MessageService messageService, UserServiceClient userServiceClient) {
        this.messageService = messageService;
        this.userServiceClient = userServiceClient;
    }

    @MessageMapping("/sendMessage") // event which will be triggered
    @SendTo("/topic/messages") // this is the queue where it is sent
    public Message sendMessage(Message message){

        message.setTimestamp(new Date());
        messageService.saveMessage("public",message.getSender(),message.getContent());
        return message;
    }

    @GetMapping("/chat/search")
    public Mono<List<User>> searchUsers(@RequestParam("query") String query){
        log.info("inside this block");
        return userServiceClient.searchUsers(query).collectList();
//        return userServiceClient.searchUsers(query).collectList().block();
    }

    @GetMapping("/chat/messages")
    public List<Message> getMessages(){
        return messageService.getMessagesForConversation("public");
    }

    @GetMapping("/chat/messages/{conversation_id}")
    public List<Message> getAllMessageForConversation(@PathVariable String conversation_id){
        return messageService.getMessagesForConversation(conversation_id);
    }

}
