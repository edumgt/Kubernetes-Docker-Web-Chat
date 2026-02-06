package org.example.profileservice.controller;

import org.example.profileservice.model.Profile;
import org.example.profileservice.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileViewsController {

    private final ProfileService profileService;


    public ProfileViewsController(ProfileService profileService) {
        this.profileService = profileService;
    }


    @PostMapping("/profile")
    public String saveProfile(@ModelAttribute Profile profile){


        profileService.save(profile);


        return "redirect:/profile";

    }


    @GetMapping("/profile")
    public String homepage(Model model, Authentication authentication){

        if(authentication == null){
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        model.addAttribute("username", username);

        Profile profile = profileService.getProfileByEmail(username);
        if(profile == null){
            profile = new Profile();
            profile.setEmail(username);
            profile.setDisplayName("");
            profile.setProfilePicUrl("https://media.pitchfork.com/photos/5c7d4c1b4101df3df85c41e5/1:1/w_800,h_800,c_limit/Dababy_BabyOnBaby.jpg");
        }

        model.addAttribute("profile",profile);
        return "newhomepage";

    }

}
