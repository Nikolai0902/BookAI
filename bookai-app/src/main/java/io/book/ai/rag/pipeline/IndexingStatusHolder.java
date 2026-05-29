package io.book.ai.rag.pipeline;

import io.book.ai.rag.api.IndexingResponse;
import org.springframework.stereotype.Component;

/**
 * Хранит текущий статус фонового процесса индексации.
 */
@Component
public class IndexingStatusHolder {

    public enum State { IDLE, RUNNING, DONE, ERROR }

    private volatile State state = State.IDLE;
    private volatile String message = "Индексирование не запускалось";
    private volatile IndexingResponse lastResult;

    /** @return текущее состояние */
    public State getState() { return state; }

    /** @return последнее сообщение о статусе или ошибке */
    public String getMessage() { return message; }

    /** @return результат последней успешной индексации */
    public IndexingResponse getLastResult() { return lastResult; }

    /** Переводит в состояние RUNNING. */
    public void markRunning() {
        state = State.RUNNING;
        message = "Индексирование выполняется...";
        lastResult = null;
    }

    /** Переводит в состояние DONE с результатом. */
    public void markDone(IndexingResponse result) {
        state = State.DONE;
        message = "Индексирование завершено";
        lastResult = result;
    }

    /** Переводит в состояние ERROR с описанием. */
    public void markError(String error) {
        state = State.ERROR;
        message = error;
        lastResult = null;
    }
}
