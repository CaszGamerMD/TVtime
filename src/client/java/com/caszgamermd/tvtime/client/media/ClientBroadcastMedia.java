package com.caszgamermd.tvtime.client.media;

import com.caszgamermd.tvtime.client.audio.TvAudioBus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-session client receive state.
 */
public final class ClientBroadcastMedia {
    private static final long DEFAULT_VIDEO_QUEUE_BYTES = 8L * 1024L * 1024L;
    private static final long DEFAULT_AUDIO_QUEUE_BYTES = 512L * 1024L;

    private static final Map<UUID, EncodedMediaQueue> QUEUES = new ConcurrentHashMap<>();

    private ClientBroadcastMedia() {
    }

    public static EncodedMediaQueue queue(UUID sessionId) {
        return QUEUES.computeIfAbsent(
            sessionId,
            ignored -> new EncodedMediaQueue(
                DEFAULT_VIDEO_QUEUE_BYTES,
                DEFAULT_AUDIO_QUEUE_BYTES
            )
        );
    }

    public static void remove(UUID sessionId) {
        EncodedMediaQueue queue = QUEUES.remove(sessionId);
        if (queue != null) {
            queue.clear();
        }
        VideoFrameStore.remove(sessionId);
        VideoDecodeScheduler.remove(sessionId);
        DeltaVideoDecoderStore.remove(sessionId);
        H264VideoDecoderStore.remove(sessionId);
        OpusAudioDecoderStore.remove(sessionId);
        TvAudioBus.removeSession(sessionId);
    }

    public static void clear() {
        QUEUES.values().forEach(EncodedMediaQueue::clear);
        QUEUES.clear();
        VideoFrameStore.clear();
        VideoDecodeScheduler.clear();
        DeltaVideoDecoderStore.clear();
        H264VideoDecoderStore.clear();
        OpusAudioDecoderStore.clear();
        TvAudioBus.clear();
    }
}
