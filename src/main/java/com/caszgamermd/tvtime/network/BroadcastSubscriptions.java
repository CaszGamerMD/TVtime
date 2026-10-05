package com.caszgamermd.tvtime.network;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BroadcastSubscriptions {
    private final ConcurrentHashMap<UUID, Set<UUID>> viewersBySession = new ConcurrentHashMap<>();

    public void subscribe(UUID sessionId, UUID playerId) {
        viewersBySession.computeIfAbsent(
            sessionId,
            ignored -> ConcurrentHashMap.newKeySet()
        ).add(playerId);
    }

    public void unsubscribe(UUID sessionId, UUID playerId) {
        Set<UUID> viewers = viewersBySession.get(sessionId);
        if (viewers == null) {
            return;
        }

        viewers.remove(playerId);
        if (viewers.isEmpty()) {
            viewersBySession.remove(sessionId, viewers);
        }
    }

    public Set<UUID> viewers(UUID sessionId) {
        Set<UUID> viewers = viewersBySession.get(sessionId);
        return viewers == null ? Collections.emptySet() : Set.copyOf(viewers);
    }

    public void removeSession(UUID sessionId) {
        viewersBySession.remove(sessionId);
    }

    public void removePlayer(UUID playerId) {
        viewersBySession.forEach((sessionId, viewers) -> {
            viewers.remove(playerId);
            if (viewers.isEmpty()) {
                viewersBySession.remove(sessionId, viewers);
            }
        });
    }
}
