package io.book.ai.ollama;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Конфигурация локальной LLM через Ollama: базовый URL, модель по умолчанию и параметры генерации.
 * Параметры из {@link Options} мапятся в {@code options} запроса Ollama в snake_case.
 */
@Component
@ConfigurationProperties(prefix = "ollama")
@Getter
@Setter
public class OllamaProperties {

    private String baseUrl = "http://localhost:11434";
    private String model = "qwen2.5:3b";
    private Options options = new Options();

    /**
     * Собирает только не-{@code null} параметры в Map с ключами в snake_case
     * (формат, который ждёт Ollama API).
     *
     * @return карта параметров для поля {@code options} запроса Ollama
     */
    public Map<String, Object> toOptionsMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (options == null) {
            return map;
        }
        putIfNotNull(map, "temperature", options.temperature);
        putIfNotNull(map, "top_p", options.topP);
        putIfNotNull(map, "top_k", options.topK);
        putIfNotNull(map, "num_ctx", options.numCtx);
        putIfNotNull(map, "num_predict", options.numPredict);
        putIfNotNull(map, "repeat_penalty", options.repeatPenalty);
        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * Параметры генерации Ollama. Имена полей в camelCase — Spring биндит из kebab-case
     * ({@code top-p} → {@code topP}). В {@link #toOptionsMap()} конвертятся в snake_case под Ollama API.
     */
    @Getter
    @Setter
    public static class Options {
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Integer numCtx;
        private Integer numPredict;
        private Double repeatPenalty;
    }
}
