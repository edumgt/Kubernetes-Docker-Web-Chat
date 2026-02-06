package org.example.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.chatservice.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;


@Slf4j
@Service
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://auth-service").build();
    }

//    public Mono<User> searchUsers(String query){
//        Mono<User> mono = webClient.get()
//                .uri(uriBuilder -> uriBuilder.path("/auth/search")
//                        .queryParam("query",query)
//                        .build())
//                .retrieve()
//                .bodyToMono(User.class);
//        log.info();
//        return mono;
//    }

    public Flux<User> searchUsers(String query){
        return webClient.get()
                .uri(
                        uriBuilder -> uriBuilder.path("/auth/search")
                                .queryParam("query",query)
                                .build()
                )
                .retrieve()
                .bodyToFlux(User.class);
    }

//    public Mono<User> getUserByEmail(String email){
//        return webClient.get()
//                .uri("/auth/users/{email}",email)
//                .retrieve()
//                .bodyToMono(User.class);
//    }

}
