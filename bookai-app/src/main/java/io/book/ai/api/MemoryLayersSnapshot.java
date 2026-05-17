package io.book.ai.api;

import java.util.Map;

public record MemoryLayersSnapshot(
        int shortTermCount,
        String workingMemory,
        Map<String, String> longTermMemory
) {}
