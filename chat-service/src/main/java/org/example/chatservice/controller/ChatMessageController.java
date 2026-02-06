package org.example.chatservice.controller;


import lombok.extern.slf4j.Slf4j;
import org.example.chatservice.model.Message;
import org.example.chatservice.model.Profile;
import org.example.chatservice.model.User;
import org.example.chatservice.service.ConversationService;
import org.example.chatservice.service.MessageService;
import org.example.chatservice.service.ProfileServiceClient;
import org.example.chatservice.service.UserServiceClient;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/chat")
public class ChatMessageController {


//    private final UserServiceClient userServiceClient;
    private final ConversationService conversationService;
    private final ProfileServiceClient profileServiceClient;

    public ChatMessageController(MessageService messageService, UserServiceClient userServiceClient, ConversationService conversationService, ProfileServiceClient profileServiceClient) {
//        this.userServiceClient = userServiceClient;
        this.conversationService = conversationService;
        this.profileServiceClient = profileServiceClient;
    }

    @GetMapping("")
    public String homepage(Model model, Authentication authentication){

        if(authentication == null){
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        model.addAttribute("username", username);

        return "newhomepage";

    }


    @GetMapping("/public")
    public String publicChat(Model model, Authentication authentication){


        if (authentication == null) {
            return "redirect:/auth/login";
        }

        String username = authentication.getName();

        model.addAttribute("username",username);

        return "newpublic-chat";
    }

//    @GetMapping("/search")
//    @ResponseBody
//    public List<User> searchUsers(@RequestParam("query") String query){
//
//        return userServiceClient.searchUsers(query).collectList().block();
//    }



    @GetMapping("/private")
    public String privateChat(@RequestParam("recipient") String recipient, Model model, Authentication authentication){

        if (authentication == null) {
//          not authentication pls redirect
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        model.addAttribute("username",username);
        model.addAttribute("recipient",recipient);
        String conversation_id = conversationService.getOrCreateConversation(username,recipient).getId();
        model.addAttribute("conversation_id",conversation_id);

        Mono<Profile> sender_profile_mono = profileServiceClient.getProfileByEmail(username);
        Mono<Profile> recipient_profile_mono = profileServiceClient.getProfileByEmail(recipient);

        Profile sender_profile = sender_profile_mono.block();
        Profile recipient_profile = recipient_profile_mono.block();
        log.info(sender_profile != null ? sender_profile.toString() : "EMPTY SENDER PROFILE");
        model.addAttribute("sender_displayname",
                (sender_profile != null && sender_profile.getDisplayName() != null)
                        ? sender_profile.getDisplayName() : username
                );

        model.addAttribute("recipient_displayname",
                (recipient_profile != null && recipient_profile.getDisplayName() != null)
                        ? recipient_profile.getDisplayName() : recipient
        );

        model.addAttribute("sender_profilepic",
                (sender_profile != null && sender_profile.getProfilePicUrl() != null)
                        ? sender_profile.getProfilePicUrl() : "https://media.pitchfork.com/photos/5c7d4c1b4101df3df85c41e5/1:1/w_800,h_800,c_limit/Dababy_BabyOnBaby.jpg"
        );

        model.addAttribute("recipient_profilepic",
                (recipient_profile != null && recipient_profile.getProfilePicUrl() != null)
                        ? recipient_profile.getProfilePicUrl() : "https://media.pitchfork.com/photos/5c7d4c1b4101df3df85c41e5/1:1/w_800,h_800,c_limit/Dababy_BabyOnBaby.jpg"
        );

        return "newprivate-chat";
    }


}
