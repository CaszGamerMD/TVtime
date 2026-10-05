package com.caszgamermd.tvtime.client.network;

import com.caszgamermd.tvtime.client.media.ClientBroadcastMedia;
import com.caszgamermd.tvtime.network.payload.MediaRelayPayload;
import com.caszgamermd.tvtime.network.payload.StartBroadcastAckPayload;

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
            MediaRelayPayload.TYPE,
            (payload, context) -> ClientBroadcastMedia
                .queue(payload.sessionId())
                .offer(payload.asChunk())
        );
    }
}
