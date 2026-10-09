package com.caszgamermd.caszualtvtime.client.media;

import com.caszgamermd.caszualtvtime.network.MediaKind;
import com.caszgamermd.caszualtvtime.network.payload.MediaFragmentPayload;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MediaReassembler {
    private static final long STALE_AFTER_MILLIS = 5_000L;
    private static final int MAX_REASSEMBLED_BYTES =
        MediaFragmentPayload.MAX_FRAGMENT_BYTES
            * MediaFragmentPayload.MAX_FRAGMENTS;

    private final Map<Key, Assembly> assemblies = new ConcurrentHashMap<>();

    public CompletedMedia accept(MediaFragmentPayload fragment) {
        cleanup();

        Key key = new Key(
            fragment.sessionId(),
            fragment.kind(),
            fragment.sequence()
        );

        Assembly assembly = assemblies.computeIfAbsent(
            key,
            ignored -> new Assembly(fragment)
        );

        CompletedMedia complete = assembly.accept(fragment);
        if (complete != null) {
            assemblies.remove(key);
        }

        return complete;
    }

    public void clear() {
        assemblies.clear();
    }

    private void cleanup() {
        long cutoff = System.currentTimeMillis() - STALE_AFTER_MILLIS;
        assemblies.entrySet().removeIf(
            entry -> entry.getValue().createdAtMillis < cutoff
        );
    }

    private record Key(
        UUID sessionId,
        MediaKind kind,
        long sequence
    ) {
    }

    public record CompletedMedia(
        UUID sessionId,
        MediaKind kind,
        long sequence,
        long presentationTimeMicros,
        boolean keyFrame,
        byte[] payload
    ) {
    }

    private static final class Assembly {
        private final long createdAtMillis = System.currentTimeMillis();
        private final MediaFragmentPayload first;
        private final byte[][] fragments;
        private int received;
        private int totalBytes;

        private Assembly(MediaFragmentPayload first) {
            this.first = first;
            this.fragments = new byte[first.fragmentCount()][];
        }

        private synchronized CompletedMedia accept(
            MediaFragmentPayload fragment
        ) {
            if (fragment.fragmentCount() != fragments.length
                || fragment.presentationTimeMicros()
                    != first.presentationTimeMicros()
                || fragment.keyFrame() != first.keyFrame()) {
                return null;
            }

            int index = fragment.fragmentIndex();
            if (fragments[index] == null) {
                fragments[index] = Arrays.copyOf(
                    fragment.payload(),
                    fragment.payload().length
                );
                received++;
                totalBytes += fragment.payload().length;
            }

            if (totalBytes > MAX_REASSEMBLED_BYTES) {
                return null;
            }

            if (received != fragments.length) {
                return null;
            }

            ByteArrayOutputStream out =
                new ByteArrayOutputStream(totalBytes);

            for (byte[] bytes : fragments) {
                out.writeBytes(bytes);
            }

            return new CompletedMedia(
                first.sessionId(),
                first.kind(),
                first.sequence(),
                first.presentationTimeMicros(),
                first.keyFrame(),
                out.toByteArray()
            );
        }
    }
}
