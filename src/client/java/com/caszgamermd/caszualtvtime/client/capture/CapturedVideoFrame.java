package com.caszgamermd.caszualtvtime.client.capture;

import java.nio.ByteBuffer;
import java.util.Objects;

public record CapturedVideoFrame(
    int width,
    int height,
    long timestampMicros,
    PixelFormat format,
    ByteBuffer pixels
) {
    public enum PixelFormat {
        BGRA8,
        RGBA8
    }

    public CapturedVideoFrame {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Captured frame dimensions must be positive");
        }
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(pixels, "pixels");
    }
}
