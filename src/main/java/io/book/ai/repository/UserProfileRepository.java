package io.book.ai.repository;

import io.book.ai.repository.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {
}
