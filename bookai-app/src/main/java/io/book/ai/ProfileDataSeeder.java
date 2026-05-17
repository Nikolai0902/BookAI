package io.book.ai;

import io.book.ai.handler.agent.AgentInvariantManager;
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
    private final AgentInvariantManager invariantManager;

    @Override
    public void run(String... args) {
        profileRepository.saveAll(List.of(
                new UserProfileEntity("default", "Default", FORMAL, PLAIN, null),
                new UserProfileEntity("alice", "Alice - PM", CASUAL, BULLETS,
                        "Keep responses short, avoid technical jargon."),
                new UserProfileEntity("dev", "Dev Profile", TECHNICAL, MARKDOWN,
                        "Include code examples. Prefer Java.")
        ));

        invariantManager.addInvariant("dev", "архитектура",
                "Java 25 + Spring Boot 4. Не предлагать другие языки или JVM-фреймворки.");
        invariantManager.addInvariant("dev", "архитектура",
                "Только REST API. GraphQL и gRPC не предлагать без явного согласования.");
        invariantManager.addInvariant("dev", "стек",
                "Maven для сборки. Gradle не предлагать.");
        invariantManager.addInvariant("dev", "стек",
                "H2 в dev-окружении, PostgreSQL в prod. NoSQL не предлагать.");

        invariantManager.addInvariant("alice", "бизнес",
                "Не предлагать платные инструменты и сервисы без обоснования стоимости.");
    }
}
