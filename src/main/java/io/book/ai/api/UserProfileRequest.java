package io.book.ai.api;

import io.book.ai.repository.entity.CommunicationStyle;
import io.book.ai.repository.entity.ResponseFormat;

public record UserProfileRequest(
        String profileId,
        String displayName,
        CommunicationStyle style,
        ResponseFormat responseFormat,
        String constraints
) {}
