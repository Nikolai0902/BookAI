package io.book.ai.ollama;

import java.util.List;

/**
 * Тело ответа от Ollama API {@code POST /api/embed}.
 *
 * @param model      идентификатор модели
 * @param embeddings список векторов — по одному на каждый входной текст
 */
public record OllamaEmbedResponse(String model, List<List<Double>> embeddings) {}
