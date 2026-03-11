package org.example.oauth.config;

import org.example.oauth.model.User;
import org.example.oauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestUserDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final boolean seedEnabled;

    public TestUserDataInitializer(
            UserRepository userRepository,
            @Value("${app.seed.test-users.enabled:true}") boolean seedEnabled
    ) {
        this.userRepository = userRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        upsertUser("test1", "test1", "123456");
        upsertUser("test2", "test2", "123456");
        upsertUser("test3", "test3", "123456");
    }

    private void upsertUser(String email, String name, String password) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setName(name);
        user.setPassword(password);
        userRepository.save(user);
    }
}
