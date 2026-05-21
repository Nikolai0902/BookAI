package io.book.ai.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.book.ai.scheduler.entity.ReminderEntity;
import io.book.ai.scheduler.entity.ReminderStatus;
import io.book.ai.scheduler.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис управления напоминаниями.
 * <p>
 * Отвечает за сохранение новых напоминаний, фоновую проверку просроченных
 * и формирование агрегированной сводки для MCP-инструмента {@code getSummary}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReminderRepository reminderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Сохраняет новое напоминание и возвращает JSON-подтверждение.
     *
     * @param text         текст напоминания
     * @param delaySeconds задержка в секундах до срабатывания (по умолчанию 60)
     * @return JSON-строка с id, текстом и запланированным временем
     */
    @Transactional
    public String addReminder(String text, int delaySeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fireAt = now.plusSeconds(delaySeconds);
        ReminderEntity reminder = reminderRepository.save(new ReminderEntity(text, now, fireAt));

        log.info("Reminder #{} добавлен: '{}', сработает в {}", reminder.getId(), text, fireAt.format(FMT));

        return toJson(Map.of(
                "id", reminder.getId(),
                "text", reminder.getText(),
                "scheduledAt", reminder.getFireAt().format(FMT),
                "delaySeconds", delaySeconds,
                "status", "PENDING"
        ));
    }

    /**
     * Возвращает агрегированную сводку: счётчики и список последних сработавших напоминаний.
     *
     * @return JSON-строка со сводкой
     */
    public String getSummary() {
        long pendingCount = reminderRepository.countByStatus(ReminderStatus.PENDING);
        long firedCount = reminderRepository.countByStatus(ReminderStatus.FIRED);

        List<Map<String, Object>> recentFired = reminderRepository
                .findTop10ByStatusOrderByFiredAtDesc(ReminderStatus.FIRED)
                .stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("text", r.getText());
                    m.put("firedAt", r.getFiredAt().format(FMT));
                    return m;
                })
                .toList();

        return toJson(Map.of(
                "pending", pendingCount,
                "fired", firedCount,
                "recentFired", recentFired
        ));
    }

    /**
     * Фоновая задача — каждые 5 секунд проверяет просроченные напоминания
     * и переводит их в статус {@link ReminderStatus#FIRED}.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processDueReminders() {
        List<ReminderEntity> due = reminderRepository
                .findByStatusAndFireAtLessThanEqual(ReminderStatus.PENDING, LocalDateTime.now());
        if (due.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (ReminderEntity reminder : due) {
            reminder.fire(now);
            reminderRepository.save(reminder);
            log.info("Reminder fired: [#{}] '{}'", reminder.getId(), reminder.getText());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
