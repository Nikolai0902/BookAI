package io.book.ai.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "agent_long_term_memory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "fact_key"}))
@Getter
@NoArgsConstructor
public class AgentLongTermMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    /** Категория: profile | decision | knowledge */
    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "fact_key", nullable = false, length = 500)
    private String key;

    @Column(name = "fact_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    private Instant updatedAt;

    public AgentLongTermMemoryEntity(String profileId, String category, String key, String value) {
        this.profileId = profileId;
        this.category = category;
        this.key = key;
        this.value = value;
        this.updatedAt = Instant.now();
    }

    public void update(String newCategory, String newValue) {
        this.category = newCategory;
        this.value = newValue;
        this.updatedAt = Instant.now();
    }
}
