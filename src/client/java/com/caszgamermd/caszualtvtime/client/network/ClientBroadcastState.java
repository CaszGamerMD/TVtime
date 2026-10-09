package com.caszgamermd.caszualtvtime.client.network;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientBroadcastState {
    public record ActiveBroadcast(UUID sessionId, String channel) {
    }

    private static final AtomicReference<ActiveBroadcast> ACTIVE = new AtomicReference<>();

    private ClientBroadcastState() {
    }

    public static void started(UUID sessionId, String channel) {
        ACTIVE.set(new ActiveBroadcast(sessionId, channel));
    }

    public static ActiveBroadcast active() {
        return ACTIVE.get();
    }

    public static void clear() {
        ACTIVE.set(null);
    }
}
