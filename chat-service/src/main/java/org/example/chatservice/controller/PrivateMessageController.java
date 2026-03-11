package org.example.chatservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.chatservice.model.Conversation;
import org.example.chatservice.model.Message;
import org.example.chatservice.model.PrivateMessageDTO;
import org.example.chatservice.service.ConversationService;
import org.example.chatservice.service.MessageService;
import org.example.chatservice.service.WebSocketDispatchService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class PrivateMessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final WebSocketDispatchService webSocketDispatchService;


    public PrivateMessageController(
            MessageService messageService,
            ConversationService conversationService,
            WebSocketDispatchService webSocketDispatchService
    ) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.webSocketDispatchService = webSocketDispatchService;
    }

    @MessageMapping("/private/chat")
    public void sendPrivateMessage(PrivateMessageDTO messageDTO){
        String sender = messageDTO.getSender();
        String recipient = messageDTO.getRecipient();
        log.info("{} {}", sender, recipient);

        Conversation conversation = conversationService.getOrCreateConversation(sender,recipient);
        String conversation_id = conversation.getId();

        log.info(conversation.toString());
        Message message = messageService.saveMessage(conversation.getId(), sender, messageDTO.getContent());
        webSocketDispatchService.dispatchPrivateMessage(conversation_id, sender, recipient, message);

    }


}
