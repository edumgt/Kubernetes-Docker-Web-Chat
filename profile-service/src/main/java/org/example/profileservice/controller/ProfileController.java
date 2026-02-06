package org.example.profileservice.controller;


import org.example.profileservice.model.Profile;
import org.example.profileservice.repository.ProfileRepository;
import org.example.profileservice.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController( ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{email}")
    public Profile getProfile(@PathVariable String email){
        return profileService.getProfileByEmail(email);
    }

}
