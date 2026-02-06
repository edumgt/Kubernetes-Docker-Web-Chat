package org.example.oauth.controller;


import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Authentication authentication){

//        if(authentication != null && authentication.isAuthenticated()){
//            return "redirect:/home";
//        }

        return "login";
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication){

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();


        String name = oAuth2User.getAttribute("name");
        String email = oAuth2User.getAttribute("email");


        model.addAttribute("username",name);
        model.addAttribute("email",email);

        return "redirect:http://localhost:9999/chat";
    }

    @GetMapping("/logout-success")
    public String logoutSuccess(Authentication authentication){

        return "logout";
    }
}
