package org.example.chatservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.chatservice.model.Conversation;
import org.example.chatservice.model.Message;
import org.example.chatservice.model.PrivateMessageDTO;
import org.example.chatservice.service.ConversationService;
import org.example.chatservice.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Date;

@Slf4j
@Controller
public class PrivateMessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate simpMessagingTemplate;


    public PrivateMessageController(MessageService messageService, ConversationService conversationService, SimpMessagingTemplate simpMessagingTemplate) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/private/chat")
    public void sendPrivateMessage(PrivateMessageDTO messageDTO){
        String sender = messageDTO.getSender();
        String recipient = messageDTO.getRecipient();
        log.info("{} {}", sender, recipient);

        Conversation conversation = conversationService.getOrCreateConversation(sender,recipient);
        String conversation_id = conversation.getId();

        log.info(conversation.toString());
        Message message = new Message();
        message.setSender(sender);
        message.setConversationId(conversation.getId());
        message.setContent(messageDTO.getContent());
        message.setTimestamp(new Date());
        messageService.saveMessage(message.getConversationId(),sender, message.getContent());


        simpMessagingTemplate.convertAndSendToUser(recipient,"/queue/private/" + conversation_id ,message);
        simpMessagingTemplate.convertAndSendToUser(sender,"/queue/private/" + conversation_id ,message);

    }


}
