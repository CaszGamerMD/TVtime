package com.caszgamermd.caszualcaszual_tv_time.client.media;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class H264VideoDecoderStore {
    private static final Map<UUID, State> STATES =
        new ConcurrentHashMap<>();

    private H264VideoDecoderStore() {
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
            boolean packetKeyFrame =
                transportKeyFrame
                    || H264VideoCodec.isKeyFrame(payload);

            boolean sequenceGap =
                state.lastSequence >= 0
                    && sequence != state.lastSequence + 1;

            if (sequenceGap) {
                state.decoder.reset();
                state.waitingForKeyFrame = true;
            }

            if (state.waitingForKeyFrame && !packetKeyFrame) {
                state.lastSequence = sequence;
                return null;
            }

            if (packetKeyFrame && state.waitingForKeyFrame) {
                state.decoder.reset();
                state.waitingForKeyFrame = false;
            }

            DecodedVideoFrame frame =
                state.decoder.decode(
                    payload,
                    presentationTimeMicros
                );

            state.lastSequence = sequence;

            if (frame == null) {
                state.decoder.reset();
                state.waitingForKeyFrame = true;
                return null;
            }

            return frame;
        }
    }

    public static void remove(UUID sessionId) {
        STATES.remove(sessionId);
    }

    public static void clear() {
        STATES.clear();
    }

    private static final class State {
        private final H264VideoCodec.Decoder decoder =
            new H264VideoCodec.Decoder();
        private long lastSequence = -1;
        private boolean waitingForKeyFrame;
    }
}
