package com.caszgamermd.caszualcaszual_tv_time.client.media;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Temporary animated source used to validate the full decoded-frame -> GPU
 * texture -> connected-display path before Windows capture/codec integration.
 */
public final class TestPatternVideo {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 180;
    private static final long FRAME_INTERVAL_NANOS = 100_000_000L; // 10 FPS test source

    private static volatile long lastFrameNanos;

    private TestPatternVideo() {
    }

    public static UUID sessionId(String channel) {
        String key = channel == null || channel.isBlank() ? "test" : channel.trim();
        return UUID.nameUUIDFromBytes(("caszual_tv_time:test:" + key).getBytes(StandardCharsets.UTF_8));
    }

    public static void publishIfDue(UUID sessionId) {
        long now = System.nanoTime();
        if (now - lastFrameNanos < FRAME_INTERVAL_NANOS) {
            return;
        }
        lastFrameNanos = now;

        ByteBuffer rgba = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4);
        int sweep = (int) ((now / 20_000_000L) % WIDTH);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int band = (x * 7) / WIDTH;

                int r;
                int g;
                int b;

                switch (band) {
                    case 0 -> { r = 255; g = 255; b = 255; }
                    case 1 -> { r = 255; g = 255; b = 0; }
                    case 2 -> { r = 0; g = 255; b = 255; }
                    case 3 -> { r = 0; g = 255; b = 0; }
                    case 4 -> { r = 255; g = 0; b = 255; }
                    case 5 -> { r = 255; g = 0; b = 0; }
                    default -> { r = 0; g = 0; b = 255; }
                }

                // Dark lower strip makes orientation obvious.
                if (y > HEIGHT * 3 / 4) {
                    r /= 3;
                    g /= 3;
                    b /= 3;
                }

                // Moving white scan line proves the texture is being updated.
                if (Math.abs(x - sweep) <= 2) {
                    r = 255;
                    g = 255;
                    b = 255;
                }

                rgba.put((byte) r);
                rgba.put((byte) g);
                rgba.put((byte) b);
                rgba.put((byte) 255);
            }
        }

        rgba.flip();
        VideoFrameStore.publish(
            sessionId,
            new DecodedVideoFrame(WIDTH, HEIGHT, now / 1_000L, rgba)
        );
    }
}
