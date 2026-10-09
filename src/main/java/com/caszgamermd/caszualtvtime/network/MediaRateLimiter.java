package com.caszgamermd.caszualtvtime.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple one-second byte budget for incoming broadcaster media.
 *
 * The advertised video bitrate is converted to bytes/sec and given modest
 * headroom for audio, codec/container overhead, and packetization.
 */
public final class MediaRateLimiter {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final long AUDIO_AND_OVERHEAD_BYTES_PER_SECOND = 64L * 1024L;
    private static final double VIDEO_HEADROOM = 1.20;

    private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(UUID sessionId, int advertisedVideoKbps, int packetBytes) {
        if (packetBytes < 0) {
            return false;
        }

        long videoBytesPerSecond = Math.max(
            1L,
            (long) Math.ceil((advertisedVideoKbps * 1000.0 / 8.0) * VIDEO_HEADROOM)
        );
        long budget = videoBytesPerSecond + AUDIO_AND_OVERHEAD_BYTES_PER_SECOND;

        Window window = windows.computeIfAbsent(sessionId, ignored -> new Window());
        return window.tryConsume(packetBytes, budget);
    }

    public void remove(UUID sessionId) {
        windows.remove(sessionId);
    }

    private static final class Window {
        private long startedNanos = System.nanoTime();
        private long bytes;

        private synchronized boolean tryConsume(long amount, long budget) {
            long now = System.nanoTime();
            if (now - startedNanos >= WINDOW_NANOS) {
                startedNanos = now;
                bytes = 0;
            }

            if (bytes + amount > budget) {
                return false;
            }

            bytes += amount;
            return true;
        }
    }
}
