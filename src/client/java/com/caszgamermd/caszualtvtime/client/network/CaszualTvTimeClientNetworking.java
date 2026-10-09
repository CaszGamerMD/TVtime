package com.caszgamermd.caszualtvtime.client.network;

import com.caszgamermd.caszualtvtime.client.media.ClientBroadcastMedia;
import com.caszgamermd.caszualtvtime.network.payload.CameraCatalogPayload;
import com.caszgamermd.caszualtvtime.client.screen.CameraControlScreen;
import net.minecraft.client.Minecraft;
import com.caszgamermd.caszualtvtime.client.media.RawTestFrameCodec;
import com.caszgamermd.caszualtvtime.client.media.VideoFrameStore;
import com.caszgamermd.caszualtvtime.client.media.VideoDecodeScheduler;
import com.caszgamermd.caszualtvtime.client.media.RawPcmAudioCodec;
import com.caszgamermd.caszualtvtime.client.audio.TvAudioBus;
import com.caszgamermd.caszualtvtime.client.media.DeflateVideoCodec;
import com.caszgamermd.caszualtvtime.client.media.DeltaVideoDecoderStore;
import com.caszgamermd.caszualtvtime.client.media.H264VideoCodec;
import com.caszgamermd.caszualtvtime.client.media.H264VideoDecoderStore;
import com.caszgamermd.caszualtvtime.client.media.OpusAudioCodec;
import com.caszgamermd.caszualtvtime.client.media.OpusAudioDecoderStore;
import com.caszgamermd.caszualtvtime.network.MediaKind;
import com.caszgamermd.caszualtvtime.client.media.MediaReassembler;
import com.caszgamermd.caszualtvtime.network.payload.MediaRelayPayload;
import com.caszgamermd.caszualtvtime.network.payload.MediaFragmentPayload;
import com.caszgamermd.caszualtvtime.network.payload.StartBroadcastAckPayload;
import com.caszgamermd.caszualtvtime.network.payload.ChannelSessionPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class CaszualTvTimeClientNetworking {
    private static final MediaReassembler REASSEMBLER = new MediaReassembler();

    private CaszualTvTimeClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
            CameraCatalogPayload.TYPE,
            (payload, context) -> context.client().execute(() -> {
                if (Minecraft.getInstance().gui.screen() instanceof CameraControlScreen screen) {
                    screen.updateCatalog(payload);
                }
            })
        );
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
