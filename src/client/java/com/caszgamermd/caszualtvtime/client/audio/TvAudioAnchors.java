package com.caszgamermd.caszualtvtime.client.audio;

import com.caszgamermd.caszualtvtime.audio.SpeakerChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TvAudioAnchors {
    private static final long STALE_AFTER_MILLIS = 5_000L;
    private static final Map<SourceKey, Anchor> ANCHORS =
        new ConcurrentHashMap<>();

    private TvAudioAnchors() {
    }

    public static void markSeen(
        UUID sessionId,
        BlockPos sourceBlock,
        Vec3 position,
        float volume,
        float range
    ) {
        markSeen(
            sessionId,
            sourceBlock,
            position,
            SpeakerChannel.FULL,
            volume,
            range
        );
    }

    public static void markSeen(
        UUID sessionId,
        BlockPos sourceBlock,
        Vec3 position,
        SpeakerChannel speakerChannel,
        float volume,
        float range
    ) {
        if (sessionId == null || sourceBlock == null || position == null) {
            return;
        }

        ANCHORS.put(
            new SourceKey(sessionId, sourceBlock.immutable()),
            new Anchor(
                position,
                speakerChannel == null
                    ? SpeakerChannel.FULL
                    : speakerChannel,
                Math.max(0.0f, Math.min(2.0f, volume)),
                Math.max(1.0f, Math.min(128.0f, range)),
                System.currentTimeMillis()
            )
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
        BlockPos sourceBlock
    ) {
    }

    public record Anchor(
        Vec3 position,
        SpeakerChannel speakerChannel,
        float volume,
        float range,
        long seenAtMillis
    ) {
    }
}
