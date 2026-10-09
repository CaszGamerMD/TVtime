package com.caszgamermd.caszualtvtime.broadcast;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BroadcastManager {
    private final Map<UUID, BroadcastSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> channels = new ConcurrentHashMap<>();

    public BroadcastSession create(UUID broadcaster, String channel) {
        if (channels.containsKey(channel)) {
            throw new IllegalStateException("Channel already in use: " + channel);
        }

        BroadcastSession session = new BroadcastSession(UUID.randomUUID(), broadcaster, channel);
        sessions.put(session.id(), session);
        channels.put(channel, session.id());
        return session;
    }

    public Optional<BroadcastSession> byId(UUID id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public Optional<BroadcastSession> byChannel(String channel) {
        UUID id = channels.get(channel);
        return id == null ? Optional.empty() : byId(id);
    }

    public Collection<BroadcastSession> all() {
        return sessions.values();
    }

    public void remove(UUID id) {
        BroadcastSession session = sessions.remove(id);
        if (session != null) {
            channels.remove(session.channel(), id);
        }
    }

    public void removeForBroadcaster(UUID broadcaster) {
        sessions.values().stream()
            .filter(session -> session.broadcaster().equals(broadcaster))
            .map(BroadcastSession::id)
            .toList()
            .forEach(this::remove);
    }
}
