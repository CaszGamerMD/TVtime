package com.caszgamermd.caszualtvtime.client.media;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps inter-frame decode state per broadcast session.
 *
 * A sequence gap invalidates delta history. The decoder then ignores delta
 * frames until the next keyframe arrives.
 */
public final class DeltaVideoDecoderStore {
    private static final Map<UUID, State> STATES =
        new ConcurrentHashMap<>();

    private DeltaVideoDecoderStore() {
    }

    public static DecodedVideoFrame decode(
        UUID sessionId,
        long sequence,
        long presentationTimeMicros,
        boolean transportKeyFrame,
        byte[] payload
    ) {
        State state = STATES.computeIfAbsent(
            sessionId,
            ignored -> new State()
        );

        synchronized (state) {
            boolean sequenceGap =
                state.lastSequence >= 0
                    && sequence != state.lastSequence + 1;

            if (sequenceGap) {
                state.previous = null;
                state.waitingForKeyFrame = true;
            }

            DeltaDeflateVideoCodec.Decoded decoded =
                DeltaDeflateVideoCodec.decode(
                    payload,
                    presentationTimeMicros,
                    state.previous
                );

            if (decoded == null) {
                return null;
            }

            boolean keyFrame =
                decoded.keyFrame() || transportKeyFrame;

            if (decoded.needsKeyFrame()) {
                state.previous = null;
                state.waitingForKeyFrame = true;
                state.lastSequence = sequence;
                return null;
            }

            if (state.waitingForKeyFrame && !keyFrame) {
                state.lastSequence = sequence;
                return null;
            }

            if (keyFrame) {
                state.waitingForKeyFrame = false;
            }

            state.previous = decoded.currentFrame();
            state.lastSequence = sequence;
            return decoded.frame();
        }
    }

    public static void remove(UUID sessionId) {
        STATES.remove(sessionId);
    }

    public static void clear() {
        STATES.clear();
    }

    private static final class State {
        private byte[] previous;
        private long lastSequence = -1;
        private boolean waitingForKeyFrame;
    }
}
