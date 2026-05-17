package io.book.ai.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor
public class UserProfileEntity {

    @Id
    @Column(nullable = false)
    private String profileId;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunicationStyle style;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponseFormat responseFormat;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(nullable = false)
    private Instant createdAt;

    public UserProfileEntity(String profileId, String displayName, CommunicationStyle style,
                             ResponseFormat responseFormat, String constraints) {
        this.profileId = profileId;
        this.displayName = displayName;
        this.style = style;
        this.responseFormat = responseFormat;
        this.constraints = constraints;
        this.createdAt = Instant.now();
    }

    public void update(String displayName, CommunicationStyle style,
                       ResponseFormat responseFormat, String constraints) {
        this.displayName = displayName;
        this.style = style;
        this.responseFormat = responseFormat;
        this.constraints = constraints;
    }
}
