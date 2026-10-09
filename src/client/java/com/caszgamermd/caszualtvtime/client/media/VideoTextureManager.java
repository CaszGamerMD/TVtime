package com.caszgamermd.caszualtvtime.client.media;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Render-thread-owned collection of broadcast textures.
 */
public final class VideoTextureManager {
    private static final Map<UUID, VideoTexture> TEXTURES = new ConcurrentHashMap<>();

    private VideoTextureManager() {
    }

    public static VideoTexture get(UUID sessionId) {
        return TEXTURES.computeIfAbsent(sessionId, VideoTexture::new);
    }

    public static void update(UUID sessionId) {
        get(sessionId).updateFromLatest();
    }

    public static void remove(UUID sessionId) {
        VideoTexture texture = TEXTURES.remove(sessionId);
        if (texture != null) {
            texture.close();
        }
    }

    public static void clear() {
        TEXTURES.values().forEach(VideoTexture::close);
        TEXTURES.clear();
    }
}
