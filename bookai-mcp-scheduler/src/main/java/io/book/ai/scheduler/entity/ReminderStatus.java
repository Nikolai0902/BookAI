package io.book.ai.scheduler.entity;

/**
 * Статус напоминания в жизненном цикле планировщика.
 */
public enum ReminderStatus {
    /** Ожидает срабатывания — {@code fireAt} ещё не наступило. */
    PENDING,
    /** Сработало — планировщик обработал и зафиксировал время срабатывания. */
    FIRED
}
