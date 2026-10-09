package com.caszgamermd.caszualtvtime.client.capture;

import java.nio.ByteBuffer;
import java.util.Objects;

public record CapturedAudioChunk(
    int sampleRate,
    int channels,
    SampleFormat format,
    long timestampMicros,
    ByteBuffer samples
) {
    public enum SampleFormat {
        S16_LE,
        F32_LE
    }

    public CapturedAudioChunk {
        if (sampleRate <= 0 || channels <= 0) {
            throw new IllegalArgumentException("Audio format must be valid");
        }
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(samples, "samples");
    }
}
