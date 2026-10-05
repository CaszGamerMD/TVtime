package com.caszgamermd.tvtime.client.audio;

import com.caszgamermd.tvtime.client.media.DecodedAudioChunk;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TvAudioBus {
    private static final int MAX_CHUNKS_PER_READER = 96;
    private static final Map<UUID, Set<Reader>> READERS =
        new ConcurrentHashMap<>();

    private TvAudioBus() {
    }

    public static Reader subscribe(UUID sessionId) {
        Reader reader = new Reader(sessionId);
        READERS.computeIfAbsent(
            sessionId,
            ignored -> ConcurrentHashMap.newKeySet()
        ).add(reader);
        return reader;
    }

    public static void offer(UUID sessionId, DecodedAudioChunk chunk) {
        Set<Reader> readers = READERS.get(sessionId);
        if (readers == null) {
            return;
        }

        for (Reader reader : readers) {
            reader.offer(chunk);
        }
    }

    public static void removeSession(UUID sessionId) {
        Set<Reader> readers = READERS.remove(sessionId);
        if (readers != null) {
            readers.forEach(Reader::closeInternal);
        }
    }

    public static void clear() {
        READERS.values().forEach(
            readers -> readers.forEach(Reader::closeInternal)
        );
        READERS.clear();
    }

    public static final class Reader implements AutoCloseable {
        private final UUID sessionId;
        private final ArrayDeque<DecodedAudioChunk> chunks =
            new ArrayDeque<>();
        private boolean closed;

        private Reader(UUID sessionId) {
            this.sessionId = sessionId;
        }

        public synchronized DecodedAudioChunk poll() {
            return chunks.pollFirst();
        }

        private synchronized void offer(DecodedAudioChunk chunk) {
            if (closed) {
                return;
            }

            while (chunks.size() >= MAX_CHUNKS_PER_READER) {
                chunks.pollFirst();
            }

            chunks.addLast(chunk);
        }

        private synchronized void closeInternal() {
            closed = true;
            chunks.clear();
        }

        @Override
        public void close() {
            closeInternal();

            Set<Reader> readers = READERS.get(sessionId);
            if (readers != null) {
                readers.remove(this);
                if (readers.isEmpty()) {
                    READERS.remove(sessionId, readers);
                }
            }
        }
    }
}
