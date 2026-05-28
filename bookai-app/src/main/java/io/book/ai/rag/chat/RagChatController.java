package io.book.ai.rag.chat;

import io.book.ai.rag.chat.api.RagChatRequest;
import io.book.ai.rag.chat.api.RagChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST-контроллер RAG-чата.
 */
@RestController
@RequestMapping("/api/rag-chat")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService chatService;

    /**
     * Выполняет один ход RAG-чата.
     *
     * @param request сообщение пользователя и параметры сессии
     * @return ответ с цитатами, памятью задачи и статистикой
     * @throws IOException если индекс недоступен
     */
    @PostMapping("/chat")
    public RagChatResponse chat(@RequestBody RagChatRequest request) throws IOException {
        return chatService.chat(request);
    }

    /**
     * Удаляет все данные сессии (сообщения и память задачи).
     *
     * @param sessionId идентификатор сессии
     */
    @DeleteMapping("/session/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
    }
}
