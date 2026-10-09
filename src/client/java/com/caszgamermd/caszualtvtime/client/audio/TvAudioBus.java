package com.caszgamermd.caszualtvtime.client.audio;

import com.caszgamermd.caszualtvtime.client.media.DecodedAudioChunk;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TvAudioBus {
    // Opus uses 20 ms packets. Keep the live queue deliberately shallow:
    // enough to absorb normal network jitter without allowing seconds of
    // latency to accumulate when the sound thread briefly falls behind.
    private static final int MAX_CHUNKS_PER_READER = 12;
    private static final int CATCH_UP_TO_CHUNKS = 6;

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

        public synchronized DecodedAudioChunk pollWaiting(
            long timeoutMillis
        ) {
            if (chunks.isEmpty() && !closed && timeoutMillis > 0) {
                try {
                    wait(timeoutMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }

            return chunks.pollFirst();
        }

        public synchronized int queuedChunks() {
            return chunks.size();
        }

        private synchronized void offer(DecodedAudioChunk chunk) {
            if (closed) {
                return;
            }

            if (chunks.size() >= MAX_CHUNKS_PER_READER) {
                while (chunks.size() > CATCH_UP_TO_CHUNKS) {
                    chunks.pollFirst();
                }
            }

            chunks.addLast(chunk);
            notifyAll();
        }

        private synchronized void closeInternal() {
            closed = true;
            chunks.clear();
            notifyAll();
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
