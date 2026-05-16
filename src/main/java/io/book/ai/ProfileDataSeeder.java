package io.book.ai;

import io.book.ai.repository.UserProfileRepository;
import io.book.ai.repository.entity.UserProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.book.ai.repository.entity.CommunicationStyle.*;
import static io.book.ai.repository.entity.ResponseFormat.*;

@Component
@RequiredArgsConstructor
public class ProfileDataSeeder implements CommandLineRunner {

    private final UserProfileRepository profileRepository;

    @Override
    public void run(String... args) {
        profileRepository.saveAll(List.of(
                new UserProfileEntity("default", "Default", FORMAL, PLAIN, null),
                new UserProfileEntity("alice", "Alice - PM", CASUAL, BULLETS,
                        "Keep responses short, avoid technical jargon."),
                new UserProfileEntity("dev", "Dev Profile", TECHNICAL, MARKDOWN,
                        "Include code examples. Prefer Java.")
        ));
    }
}
