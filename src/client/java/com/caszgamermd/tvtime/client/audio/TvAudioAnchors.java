package com.caszgamermd.tvtime.client.audio;

import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TvAudioAnchors {
    private static final long STALE_AFTER_MILLIS = 5_000L;
    private static final Map<UUID, Anchor> ANCHORS = new ConcurrentHashMap<>();

    private TvAudioAnchors() {
    }

    public static void markSeen(UUID sessionId, Vec3 position) {
        if (sessionId == null || position == null) {
            return;
        }

        ANCHORS.put(
            sessionId,
            new Anchor(position, System.currentTimeMillis())
        );
    }

    public static Map<UUID, Anchor> active() {
        long cutoff = System.currentTimeMillis() - STALE_AFTER_MILLIS;
        ANCHORS.entrySet().removeIf(entry -> entry.getValue().seenAtMillis() < cutoff);
        return Map.copyOf(ANCHORS);
    }

    public static void clear() {
        ANCHORS.clear();
    }

    public record Anchor(
        Vec3 position,
        long seenAtMillis
    ) {
    }
}
