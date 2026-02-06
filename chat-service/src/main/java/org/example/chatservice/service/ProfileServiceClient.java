package org.example.chatservice.service;


import org.example.chatservice.model.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ProfileServiceClient {

    private final WebClient webClient;

    public ProfileServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://profile-service").build();
    }

    public Mono<Profile> getProfileByEmail(String email){
        return webClient.get()
                .uri("/profile/{email}",email)
                .retrieve()
                .bodyToMono(Profile.class);
    }

}
