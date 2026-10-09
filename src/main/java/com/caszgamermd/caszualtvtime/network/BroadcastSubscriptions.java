package com.caszgamermd.caszualtvtime.network;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BroadcastSubscriptions {
    private final ConcurrentHashMap<UUID, Set<UUID>> viewersBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<UUID>> viewersByChannel = new ConcurrentHashMap<>();

    public void subscribeChannel(String channel, UUID playerId) {
        viewersByChannel.computeIfAbsent(
            normalize(channel),
            ignored -> ConcurrentHashMap.newKeySet()
        ).add(playerId);
    }

    public void unsubscribeChannel(String channel, UUID playerId) {
        String normalized = normalize(channel);
        Set<UUID> viewers = viewersByChannel.get(normalized);
        if (viewers == null) {
            return;
        }

        viewers.remove(playerId);
        if (viewers.isEmpty()) {
            viewersByChannel.remove(normalized, viewers);
        }
    }

    public Set<UUID> viewersForChannel(String channel) {
        Set<UUID> viewers = viewersByChannel.get(normalize(channel));
        return viewers == null ? Collections.emptySet() : Set.copyOf(viewers);
    }

    public void attachSession(UUID sessionId, String channel) {
        Set<UUID> waiting = viewersForChannel(channel);
        if (!waiting.isEmpty()) {
            viewersBySession
                .computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet())
                .addAll(waiting);
        }
    }

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

        viewersByChannel.forEach((channel, viewers) -> {
            viewers.remove(playerId);
            if (viewers.isEmpty()) {
                viewersByChannel.remove(channel, viewers);
            }
        });
    }

    private static String normalize(String channel) {
        return channel == null ? "" : channel.trim();
    }
}
