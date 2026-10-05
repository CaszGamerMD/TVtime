package com.caszgamermd.tvtime.client.network;

import com.caszgamermd.tvtime.network.payload.SubscribeChannelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks channels currently needed by rendered TVs.
 *
 * Rendering marks channels as seen. A lightweight client tick expires channels
 * that have not been visible recently and sends one unsubscribe packet.
 */
public final class ClientChannelSubscriptions {
    private static final long STALE_AFTER_MILLIS = 5_000L;

    private static final Map<String, Long> LAST_SEEN = new HashMap<>();
    private static final Set<String> SUBSCRIBED = new HashSet<>();

    private ClientChannelSubscriptions() {
    }

    public static synchronized void markSeen(String channel) {
        String normalized = normalize(channel);
        if (normalized.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        LAST_SEEN.put(normalized, now);

        if (SUBSCRIBED.add(normalized) && ClientPlayNetworking.canSend(SubscribeChannelPayload.TYPE)) {
            ClientPlayNetworking.send(new SubscribeChannelPayload(normalized, true));
        }
    }

    public static synchronized void tick() {
        long cutoff = System.currentTimeMillis() - STALE_AFTER_MILLIS;

        var iterator = LAST_SEEN.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() >= cutoff) {
                continue;
            }

            String channel = entry.getKey();
            iterator.remove();

            if (SUBSCRIBED.remove(channel)
                && ClientPlayNetworking.canSend(SubscribeChannelPayload.TYPE)) {
                ClientPlayNetworking.send(new SubscribeChannelPayload(channel, false));
            }
        }
    }

    public static synchronized void clear() {
        LAST_SEEN.clear();
        SUBSCRIBED.clear();
    }

    private static String normalize(String channel) {
        return channel == null ? "" : channel.trim();
    }
}
