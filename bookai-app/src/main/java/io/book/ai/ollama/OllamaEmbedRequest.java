package io.book.ai.ollama;

import java.util.List;

/**
 * Тело запроса к Ollama API {@code POST /api/embed}.
 *
 * @param model идентификатор embedding-модели (например {@code nomic-embed-text})
 * @param input список текстов для векторизации
 */
public record OllamaEmbedRequest(String model, List<String> input) {}
