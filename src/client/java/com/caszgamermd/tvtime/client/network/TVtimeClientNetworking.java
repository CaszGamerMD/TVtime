package com.caszgamermd.tvtime.client.network;

import com.caszgamermd.tvtime.client.media.ClientBroadcastMedia;
import com.caszgamermd.tvtime.client.media.RawTestFrameCodec;
import com.caszgamermd.tvtime.client.media.VideoFrameStore;
import com.caszgamermd.tvtime.network.payload.MediaRelayPayload;
import com.caszgamermd.tvtime.network.payload.StartBroadcastAckPayload;
import com.caszgamermd.tvtime.network.payload.ChannelSessionPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TVtimeClientNetworking {
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
            MediaRelayPayload.TYPE,
            (payload, context) -> {
                var rawFrame = RawTestFrameCodec.decode(
                    payload.payload(),
                    payload.presentationTimeMicros()
                );

                if (rawFrame != null) {
                    VideoFrameStore.publish(payload.sessionId(), rawFrame);
                    return;
                }

                ClientBroadcastMedia.queue(payload.sessionId()).offer(payload.asChunk());
            }
        );
    }
}
