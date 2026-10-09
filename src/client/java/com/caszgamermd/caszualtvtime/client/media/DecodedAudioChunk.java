package com.caszgamermd.caszualtvtime.client.media;

import java.nio.ByteBuffer;
import java.util.Objects;

public record DecodedAudioChunk(
    int sampleRate,
    int channels,
    long presentationTimeMicros,
    ByteBuffer samples
) {
    public DecodedAudioChunk {
        if (sampleRate <= 0 || channels <= 0) {
            throw new IllegalArgumentException("Audio format must be valid");
        }
        Objects.requireNonNull(samples, "samples");
    }
}
