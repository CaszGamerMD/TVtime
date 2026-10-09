package com.caszgamermd.caszualcaszual_tv_time.network;

import java.util.Objects;
import java.util.UUID;

public record MediaChunk(
    UUID sessionId,
    MediaKind kind,
    long sequence,
    long presentationTimeMicros,
    boolean keyFrame,
    byte[] payload
) {
    public MediaChunk {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(payload);
    }
}
