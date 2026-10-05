package com.caszgamermd.tvtime.broadcast;

import java.util.Objects;
import java.util.UUID;

public final class BroadcastSession {
    private final UUID id;
    private final UUID broadcaster;
    private final String channel;
    private BroadcastStatus status;
    private int width;
    private int height;
    private int fps;
    private int videoBitrateKbps;

    public BroadcastSession(UUID id, UUID broadcaster, String channel) {
        this.id = Objects.requireNonNull(id);
        this.broadcaster = Objects.requireNonNull(broadcaster);
        this.channel = Objects.requireNonNull(channel);
        this.status = BroadcastStatus.STARTING;
    }

    public UUID id() { return id; }
    public UUID broadcaster() { return broadcaster; }
    public String channel() { return channel; }
    public BroadcastStatus status() { return status; }
    public int width() { return width; }
    public int height() { return height; }
    public int fps() { return fps; }
    public int videoBitrateKbps() { return videoBitrateKbps; }

    public void markLive(int width, int height, int fps, int videoBitrateKbps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.videoBitrateKbps = videoBitrateKbps;
        this.status = BroadcastStatus.LIVE;
    }

    public void beginStopping() {
        this.status = BroadcastStatus.STOPPING;
    }
}
