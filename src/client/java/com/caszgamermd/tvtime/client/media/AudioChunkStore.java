package com.caszgamermd.tvtime.client.media;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioChunkStore {
    private static final int MAX_CHUNKS_PER_SESSION = 96;

    private static final Map<UUID, Queue> QUEUES = new ConcurrentHashMap<>();

    private AudioChunkStore() {
    }

    public static void offer(UUID sessionId, DecodedAudioChunk chunk) {
        QUEUES.computeIfAbsent(sessionId, ignored -> new Queue()).offer(chunk);
    }

    public static DecodedAudioChunk poll(UUID sessionId) {
        Queue queue = QUEUES.get(sessionId);
        return queue == null ? null : queue.poll();
    }

    public static void remove(UUID sessionId) {
        QUEUES.remove(sessionId);
    }

    public static void clear() {
        QUEUES.clear();
    }

    private static final class Queue {
        private final ArrayDeque<DecodedAudioChunk> chunks = new ArrayDeque<>();

        private synchronized void offer(DecodedAudioChunk chunk) {
            while (chunks.size() >= MAX_CHUNKS_PER_SESSION) {
                chunks.pollFirst();
            }
            chunks.addLast(chunk);
        }

        private synchronized DecodedAudioChunk poll() {
            return chunks.pollFirst();
        }
    }
}
