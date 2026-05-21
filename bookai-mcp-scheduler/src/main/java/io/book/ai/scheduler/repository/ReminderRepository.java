package io.book.ai.scheduler.repository;

import io.book.ai.scheduler.entity.ReminderEntity;
import io.book.ai.scheduler.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий напоминаний.
 */
public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {

    /**
     * Возвращает напоминания с заданным статусом, время срабатывания которых не позже указанного.
     * Используется планировщиком для поиска просроченных {@code PENDING}-напоминаний.
     */
    List<ReminderEntity> findByStatusAndFireAtLessThanEqual(ReminderStatus status, LocalDateTime fireAt);

    /** Возвращает последние {@code limit} напоминаний в статусе {@code FIRED}, отсортированных по времени срабатывания. */
    List<ReminderEntity> findTop10ByStatusOrderByFiredAtDesc(ReminderStatus status);

    long countByStatus(ReminderStatus status);
}
