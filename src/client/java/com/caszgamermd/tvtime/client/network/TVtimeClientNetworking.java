package com.caszgamermd.tvtime.client.network;

import com.caszgamermd.tvtime.client.media.ClientBroadcastMedia;
import com.caszgamermd.tvtime.client.media.RawTestFrameCodec;
import com.caszgamermd.tvtime.client.media.VideoFrameStore;
import com.caszgamermd.tvtime.client.media.RawPcmAudioCodec;
import com.caszgamermd.tvtime.client.audio.TvAudioBus;
import com.caszgamermd.tvtime.client.media.DeflateVideoCodec;
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
                    complete.presentationTimeMicros(),
                    complete.payload()
                );
            }
        );

        ClientPlayNetworking.registerGlobalReceiver(
            MediaRelayPayload.TYPE,
            (payload, context) -> {
                if (handleCompleteMedia(
                    payload.sessionId(),
                    payload.presentationTimeMicros(),
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
        long presentationTimeMicros,
        byte[] encoded
    ) {
        var compressedFrame = DeflateVideoCodec.decode(
            encoded,
            presentationTimeMicros
        );

        if (compressedFrame != null) {
            VideoFrameStore.publish(sessionId, compressedFrame);
            return true;
        }

        var rawFrame = RawTestFrameCodec.decode(
            encoded,
            presentationTimeMicros
        );

        if (rawFrame != null) {
            VideoFrameStore.publish(sessionId, rawFrame);
            return true;
        }

        var rawAudio = RawPcmAudioCodec.decode(
            encoded,
            presentationTimeMicros
        );

        if (rawAudio != null) {
            AudioChunkStore.offer(sessionId, rawAudio);
            return true;
        }

        return false;
    }
}
