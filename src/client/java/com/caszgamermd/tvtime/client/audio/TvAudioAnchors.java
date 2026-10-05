package com.caszgamermd.tvtime.client.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TvAudioAnchors {
    private static final long STALE_AFTER_MILLIS = 5_000L;
    private static final Map<SourceKey, Anchor> ANCHORS = new ConcurrentHashMap<>();

    private TvAudioAnchors() {
    }

    public static void markSeen(
        UUID sessionId,
        BlockPos sourcePos,
        Vec3 position
    ) {
        if (sessionId == null || sourcePos == null || position == null) {
            return;
        }

        ANCHORS.put(
            new SourceKey(sessionId, sourcePos.immutable()),
            new Anchor(sessionId, position, System.currentTimeMillis())
        );
    }

    public static Map<SourceKey, Anchor> active() {
        long cutoff = System.currentTimeMillis() - STALE_AFTER_MILLIS;
        ANCHORS.entrySet().removeIf(
            entry -> entry.getValue().seenAtMillis() < cutoff
        );
        return Map.copyOf(ANCHORS);
    }

    public static void clear() {
        ANCHORS.clear();
    }

    public record SourceKey(
        UUID sessionId,
        BlockPos sourcePos
    ) {
    }

    public record Anchor(
        UUID sessionId,
        Vec3 position,
        long seenAtMillis
    ) {
    }
}
