package io.book.ai.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурационные свойства RAG-пайплайна (префикс {@code rag}).
 * Значения берутся из {@code application.yml} и переменных окружения.
 */
@Component
@ConfigurationProperties(prefix = "rag")
@Getter
@Setter
public class RagProperties {

    private String voyageApiKey = "";
    private String voyageModel = "voyage-3-lite";
    private int embeddingDims = 512;
    private int batchSize = 64;
    private long requestDelayMs = 21000;
    private int fixedChunkSize = 600;
    private int fixedChunkOverlap = 100;
    private String indexDir = "./data/rag";
    private List<String> sourcePaths = new ArrayList<>();
}
