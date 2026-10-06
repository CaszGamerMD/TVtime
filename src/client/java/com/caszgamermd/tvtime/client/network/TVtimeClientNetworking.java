package com.caszgamermd.tvtime.client.network;

import com.caszgamermd.tvtime.client.media.ClientBroadcastMedia;
import com.caszgamermd.tvtime.client.media.RawTestFrameCodec;
import com.caszgamermd.tvtime.client.media.VideoFrameStore;
import com.caszgamermd.tvtime.client.media.VideoDecodeScheduler;
import com.caszgamermd.tvtime.client.media.RawPcmAudioCodec;
import com.caszgamermd.tvtime.client.audio.TvAudioBus;
import com.caszgamermd.tvtime.client.media.DeflateVideoCodec;
import com.caszgamermd.tvtime.client.media.DeltaVideoDecoderStore;
import com.caszgamermd.tvtime.client.media.H264VideoCodec;
import com.caszgamermd.tvtime.client.media.H264VideoDecoderStore;
import com.caszgamermd.tvtime.client.media.OpusAudioCodec;
import com.caszgamermd.tvtime.client.media.OpusAudioDecoderStore;
import com.caszgamermd.tvtime.network.MediaKind;
import com.caszgamermd.tvtime.client.media.MediaReassembler;
import com.caszgamermd.tvtime.network.payload.MediaRelayPayload;
import com.caszgamermd.tvtime.network.payload.MediaFragmentPayload;
import com.caszgamermd.tvtime.network.payload.StartBroadcastAckPayload;
import com.caszgamermd.tvtime.network.payload.ChannelSessionPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TVtimeClientNetworking {
    private static final MediaReassembler REASSEMBLER = new MediaReassembler();

    private TVtimeClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
            StartBroadcastAckPayload.TYPE,
            (payload, context) -> context.client().execute(() ->
                ClientBroadcastState.started(payload.sessionId(), payload.channel())
            )
        );

        ClientPlayNetworking.registerGlobalReceiver(
            ChannelSessionPayload.TYPE,
            (payload, context) -> context.client().execute(() ->
                ClientChannelDirectory.update(
                    payload.channel(),
                    payload.sessionId(),
                    payload.active()
                )
            )
        );

        ClientPlayNetworking.registerGlobalReceiver(
            MediaFragmentPayload.TYPE,
            (payload, context) -> {
                var complete = REASSEMBLER.accept(payload);
                if (complete == null) {
                    return;
                }

                handleCompleteMedia(
                    complete.sessionId(),
                    complete.kind(),
                    complete.sequence(),
                    complete.presentationTimeMicros(),
                    complete.keyFrame(),
                    complete.payload()
                );
            }
        );

        ClientPlayNetworking.registerGlobalReceiver(
            MediaRelayPayload.TYPE,
            (payload, context) -> {
                if (handleCompleteMedia(
                    payload.sessionId(),
                    payload.kind(),
                    payload.sequence(),
                    payload.presentationTimeMicros(),
                    payload.keyFrame(),
                    payload.payload()
                )) {
                    return;
                }

                ClientBroadcastMedia.queue(payload.sessionId()).offer(payload.asChunk());
            }
        );
    }

    private static boolean handleCompleteMedia(
        java.util.UUID sessionId,
        MediaKind kind,
        long sequence,
        long presentationTimeMicros,
        boolean keyFrame,
        byte[] encoded
    ) {
        if (kind == MediaKind.VIDEO) {
            VideoDecodeScheduler.submit(
                sessionId,
                sequence,
                presentationTimeMicros,
                keyFrame,
                encoded
            );
            return true;
        }

        if (OpusAudioCodec.isOpusPacket(encoded)) {
            var opusChunks = OpusAudioDecoderStore.decode(
                sessionId,
                sequence,
                presentationTimeMicros,
                encoded
            );

            for (var chunk : opusChunks) {
                TvAudioBus.offer(sessionId, chunk);
            }

            return true;
        }

        var rawAudio = RawPcmAudioCodec.decode(
            encoded,
            presentationTimeMicros
        );

        if (rawAudio != null) {
            TvAudioBus.offer(sessionId, rawAudio);
            return true;
        }

        return false;
    }
}
