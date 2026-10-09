package com.caszgamermd.caszualcaszual_tv_time.client.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OpusAudioDecoderStore {
    private static final int MAX_CONCEALED_PACKETS = 3;
    private static final long FRAME_MICROS = 20_000L;

    private static final Map<UUID, State> STATES =
        new ConcurrentHashMap<>();

    private OpusAudioDecoderStore() {
    }

    public static List<DecodedAudioChunk> decode(
        UUID sessionId,
        long sequence,
        long presentationTimeMicros,
        byte[] payload
    ) {
        State state = STATES.computeIfAbsent(
            sessionId,
            ignored -> new State()
        );

        synchronized (state) {
            if (state.lastSequence >= 0
                && sequence <= state.lastSequence) {
                return List.of();
            }

            List<DecodedAudioChunk> chunks = new ArrayList<>();

            if (state.lastSequence >= 0) {
                long missing = sequence - state.lastSequence - 1L;

                if (missing > 0
                    && missing <= MAX_CONCEALED_PACKETS) {
                    long firstMissingPts =
                        presentationTimeMicros
                            - missing * FRAME_MICROS;

                    for (int i = 0; i < missing; i++) {
                        DecodedAudioChunk concealed =
                            state.decoder.concealLoss(
                                firstMissingPts
                                    + i * FRAME_MICROS
                            );

                        if (concealed != null) {
                            chunks.add(concealed);
                        }
                    }
                } else if (missing > MAX_CONCEALED_PACKETS) {
                    state.decoder.reset();
                }
            }

            DecodedAudioChunk decoded = state.decoder.decode(
                payload,
                presentationTimeMicros
            );

            state.lastSequence = sequence;

            if (decoded != null) {
                chunks.add(decoded);
            }

            return List.copyOf(chunks);
        }
    }

    public static void remove(UUID sessionId) {
        STATES.remove(sessionId);
    }

    public static void clear() {
        STATES.clear();
    }

    private static final class State {
        private final OpusAudioCodec.Decoder decoder =
            new OpusAudioCodec.Decoder();
        private long lastSequence = -1;
    }
}
