package com.caszgamermd.tvtime.client.media;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-light handoff between decoder workers and Minecraft's render thread.
 *
 * Each broadcast keeps only the newest decoded frame. If rendering cannot keep
 * up, stale frames are intentionally dropped rather than building latency.
 */
public final class VideoFrameStore {
    private static final ConcurrentHashMap<UUID, AtomicReference<DecodedVideoFrame>> FRAMES =
        new ConcurrentHashMap<>();

    private VideoFrameStore() {
    }

    public static void publish(UUID sessionId, DecodedVideoFrame frame) {
        FRAMES.computeIfAbsent(sessionId, ignored -> new AtomicReference<>()).set(frame);
    }

    public static DecodedVideoFrame latest(UUID sessionId) {
        AtomicReference<DecodedVideoFrame> ref = FRAMES.get(sessionId);
        return ref == null ? null : ref.get();
    }

    public static DecodedVideoFrame takeLatest(UUID sessionId) {
        AtomicReference<DecodedVideoFrame> ref = FRAMES.get(sessionId);
        return ref == null ? null : ref.getAndSet(null);
    }

    public static void remove(UUID sessionId) {
        FRAMES.remove(sessionId);
    }

    public static void clear() {
        FRAMES.clear();
    }
}
