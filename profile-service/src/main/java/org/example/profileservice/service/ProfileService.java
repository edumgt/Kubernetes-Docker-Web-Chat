package org.example.profileservice.service;

import org.example.profileservice.model.Profile;
import org.example.profileservice.repository.ProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;


    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile getProfileByEmail(String email){
        return profileRepository.findByEmail(email);

    }

    public Profile save(Profile profile) {

        Profile existing = profileRepository.findByEmail(profile.getEmail());
        if(existing != null){
            existing.setDisplayName(profile.getDisplayName());
            existing.setProfilePicUrl(profile.getProfilePicUrl());
            return profileRepository.save(existing);
        }

        return profileRepository.save(profile);
    }
}
