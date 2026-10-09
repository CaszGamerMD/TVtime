package com.caszgamermd.caszualtvtime.client.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientChannelDirectory {
    private static final Map<String, UUID> SESSIONS = new ConcurrentHashMap<>();

    private ClientChannelDirectory() {
    }

    public static void update(String channel, UUID sessionId, boolean active) {
        String normalized = channel == null ? "" : channel.trim();
        if (normalized.isEmpty()) {
            return;
        }

        if (active) {
            SESSIONS.put(normalized, sessionId);
        } else {
            SESSIONS.remove(normalized, sessionId);
        }
    }

    public static UUID sessionFor(String channel) {
        if (channel == null) {
            return null;
        }
        return SESSIONS.get(channel.trim());
    }

    public static void clear() {
        SESSIONS.clear();
    }
}
