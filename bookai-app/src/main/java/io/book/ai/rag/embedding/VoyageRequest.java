package io.book.ai.rag.embedding;

import java.util.List;

/**
 * Запрос на генерацию эмбеддингов через Voyage AI API.
 *
 * @param input список текстов для векторизации (максимум 128 в одном батче)
 * @param model идентификатор модели, например {@code voyage-3-lite}
 */
public record VoyageRequest(List<String> input, String model) {}
