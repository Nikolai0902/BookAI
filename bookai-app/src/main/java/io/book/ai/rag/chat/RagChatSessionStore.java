package io.book.ai.rag.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.rag.api.RagQueryResponse.Citation;
import io.book.ai.rag.chat.entity.RagChatContextFactsEntity;
import io.book.ai.rag.chat.entity.RagChatMessageEntity;
import io.book.ai.rag.chat.repository.RagChatContextFactsRepository;
import io.book.ai.rag.chat.repository.RagChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Единая точка доступа к данным RAG-чат сессий:
 * сообщения ({@code rag_chat_messages}) и память задачи ({@code rag_chat_context_facts}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagChatSessionStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Citation>> CITATION_TYPE = new TypeReference<>() {};

    private final RagChatMessageRepository messageRepo;
    private final RagChatContextFactsRepository factsRepo;

    /**
     * Загружает последние {@code limit} сообщений сессии в хронологическом порядке.
     *
     * @param sessionId идентификатор сессии
     * @param limit     максимальное число сообщений
     * @return список объектов {@link Message} для передачи в LLM
     */
    public List<Message> loadRecentMessages(String sessionId, int limit) {
        List<RagChatMessageEntity> raw = messageRepo.findTop10BySessionIdOrderByCreatedAtDesc(sessionId);
        List<RagChatMessageEntity> limited = raw.stream().limit(limit).toList();
        List<RagChatMessageEntity> chronological = new java.util.ArrayList<>(limited);
        Collections.reverse(chronological);
        return chronological.stream()
                .map(e -> new Message(e.getRole(), e.getContent()))
                .toList();
    }

    /**
     * Возвращает текущую память задачи сессии.
     *
     * @param sessionId идентификатор сессии
     * @return блок фактов {@code key: value} или {@code null} если памяти ещё нет
     */
    public String loadContextFacts(String sessionId) {
        return factsRepo.findBySessionId(sessionId)
                .map(RagChatContextFactsEntity::getFacts)
                .orElse(null);
    }

    /**
     * Сохраняет сообщение пользователя.
     *
     * @param sessionId идентификатор сессии
     * @param content   текст сообщения
     */
    public void saveUserMessage(String sessionId, String content) {
        messageRepo.save(new RagChatMessageEntity(sessionId, "user", content));
    }

    /**
     * Сохраняет ответ ассистента с токен-статистикой и цитатами.
     *
     * @param sessionId    идентификатор сессии
     * @param content      текст ответа
     * @param citations    список цитат
     * @param inputTokens  токены запроса
     * @param outputTokens токены ответа
     */
    public void saveAssistantMessage(String sessionId, String content,
                                     List<Citation> citations, int inputTokens, int outputTokens) {
        String citationsJson = serializeCitations(citations);
        messageRepo.save(new RagChatMessageEntity(sessionId, content, citationsJson, inputTokens, outputTokens));
    }

    /**
     * Сохраняет или обновляет память задачи для сессии (upsert).
     *
     * @param sessionId идентификатор сессии
     * @param facts     новый блок фактов
     */
    @Transactional
    public void saveContextFacts(String sessionId, String facts) {
        RagChatContextFactsEntity entity = factsRepo.findBySessionId(sessionId)
                .orElseGet(() -> new RagChatContextFactsEntity(sessionId, facts));
        entity.updateFacts(facts);
        factsRepo.save(entity);
    }

    /**
     * Возвращает количество сообщений в сессии (для расчёта номера хода).
     *
     * @param sessionId идентификатор сессии
     * @return количество сообщений
     */
    public long getMessageCount(String sessionId) {
        return messageRepo.countBySessionId(sessionId);
    }

    /**
     * Удаляет все данные сессии: сообщения и память задачи.
     *
     * @param sessionId идентификатор сессии
     */
    @Transactional
    public void deleteSession(String sessionId) {
        messageRepo.deleteBySessionId(sessionId);
        factsRepo.deleteBySessionId(sessionId);
        log.info("Deleted RAG chat session: {}", sessionId);
    }

    private String serializeCitations(List<Citation> citations) {
        if (citations == null || citations.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(citations);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize citations: {}", e.getMessage());
            return null;
        }
    }
}
