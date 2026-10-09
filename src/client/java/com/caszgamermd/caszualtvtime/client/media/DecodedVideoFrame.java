package com.caszgamermd.caszualcaszual_tv_time.client.media;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Immutable decoded RGBA frame ready for GPU upload.
 * Pixels are tightly packed RGBA8, row-major, top-to-bottom.
 */
public record DecodedVideoFrame(
    int width,
    int height,
    long presentationTimeMicros,
    ByteBuffer rgba
) {
    public DecodedVideoFrame {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Frame dimensions must be positive");
        }
        Objects.requireNonNull(rgba, "rgba");

        long expected = (long) width * height * 4L;
        if (expected > Integer.MAX_VALUE || rgba.remaining() != (int) expected) {
            throw new IllegalArgumentException(
                "RGBA buffer length does not match " + width + "x" + height
            );
        }

        rgba = rgba.asReadOnlyBuffer();
    }
}
