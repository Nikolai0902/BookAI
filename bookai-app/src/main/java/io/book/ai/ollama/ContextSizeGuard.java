package io.book.ai.ollama;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Утилита для приблизительной оценки длины входа в токенах и защиты от превышения {@code num_ctx}.
 *
 * <p>Без этой проверки Ollama при переполнении контекстного окна молча обрезает начало промпта,
 * и пользователь видит странный ответ без понятной причины. Здесь сравниваем приблизительный
 * размер входа с лимитом и возвращаем {@code 400 Bad Request} с понятным сообщением.
 *
 * <p>Эвристика: {@code токены ≈ длина_строки / 3.5} — для русского текста этого достаточно
 * чтобы отсечь явно превышающие запросы. Точная токенизация модели не нужна.
 */
public final class ContextSizeGuard {

    private static final double CHARS_PER_TOKEN = 3.5;
    private static final int RESERVED_FOR_RESPONSE = 512;

    private ContextSizeGuard() {}

    /**
     * Грубая оценка числа токенов в строке.
     *
     * @param text исходный текст
     * @return оценка количества токенов
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * Складывает оценки по списку строк.
     *
     * @param parts произвольный набор строк (system prompt, история, текущее сообщение)
     * @return суммарная оценка токенов
     */
    public static int estimateTokens(List<String> parts) {
        int sum = 0;
        for (String p : parts) {
            sum += estimateTokens(p);
        }
        return sum;
    }

    /**
     * Бросает {@code 400 Bad Request} если суммарная оценка превышает контекстное окно модели.
     * Резервирует {@value #RESERVED_FOR_RESPONSE} токенов под ответ — лимит фактически
     * проверяется как {@code estimated + 512 > maxCtx}.
     *
     * @param estimated приблизительная длина входа в токенах
     * @param maxCtx размер окна модели (значение {@code num_ctx})
     */
    public static void check(int estimated, int maxCtx) {
        int withResponse = estimated + RESERVED_FOR_RESPONSE;
        if (withResponse > maxCtx) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Контекст ~" + estimated + " токенов (+" + RESERVED_FOR_RESPONSE
                            + " на ответ) превышает num_ctx=" + maxCtx
                            + ". Сократите историю диалога или увеличьте num_ctx.");
        }
    }
}
