package org.example.chatservice.controller;

import org.example.chatservice.model.ConversationSummaryDTO;
import org.example.chatservice.model.PrivateConversationDTO;
import org.example.chatservice.model.ReadReceiptEvent;
import org.example.chatservice.service.ConversationReadService;
import org.example.chatservice.service.ConversationService;
import org.example.chatservice.service.WebSocketDispatchService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/chat/conversations")
public class ConversationController {

    private final ConversationReadService conversationReadService;
    private final ConversationService conversationService;
    private final WebSocketDispatchService webSocketDispatchService;

    public ConversationController(
            ConversationReadService conversationReadService,
            ConversationService conversationService,
            WebSocketDispatchService webSocketDispatchService
    ) {
        this.conversationReadService = conversationReadService;
        this.conversationService = conversationService;
        this.webSocketDispatchService = webSocketDispatchService;
    }

    @GetMapping("/unread")
    public List<ConversationSummaryDTO> getUnreadConversations(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return conversationReadService.getConversationSummaries(authentication.getName());
    }

    @GetMapping("/private")
    public PrivateConversationDTO getOrCreatePrivateConversation(
            @RequestParam("recipient") String recipient,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (recipient == null || recipient.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipient is required");
        }

        String sender = authentication.getName();
        String conversationId = conversationService.getOrCreateConversation(sender, recipient).getId();
        return new PrivateConversationDTO(conversationId, sender, recipient);
    }

    @PostMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.OK)
    public ReadReceiptEvent markConversationAsRead(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        try {
            ConversationReadService.MarkReadResult result = conversationReadService.markConversationAsRead(
                    conversationId,
                    authentication.getName()
            );
            webSocketDispatchService.dispatchReadReceipt(
                    result.receipt(),
                    result.conversation().getUser1(),
                    result.conversation().getUser2()
            );
            return result.receipt();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
