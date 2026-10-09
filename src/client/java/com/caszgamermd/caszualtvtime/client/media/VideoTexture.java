package com.caszgamermd.caszualcaszual_tv_time.client.media;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * GPU-backed texture for one decoded broadcast.
 *
 * The decoder never touches this object. Frames are published to VideoFrameStore;
 * the Minecraft render thread calls updateFromLatest() to perform GPU uploads.
 */
public final class VideoTexture implements AutoCloseable {
    private final UUID sessionId;
    private final Identifier textureId;

    private DynamicTexture texture;
    private NativeImage pixels;
    private int width;
    private int height;
    private long presentationTimeMicros = Long.MIN_VALUE;

    public VideoTexture(UUID sessionId) {
        this.sessionId = sessionId;
        this.textureId = Identifier.fromNamespaceAndPath(
            "caszual_tv_time",
            "stream/" + sessionId.toString().replace("-", "")
        );
    }

    public UUID sessionId() {
        return sessionId;
    }

    public Identifier textureId() {
        return textureId;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean ready() {
        return texture != null && width > 0 && height > 0;
    }

    /**
     * Consumes the newest decoded frame, intentionally skipping stale frames.
     * Must be called on Minecraft's render thread.
     */
    public boolean updateFromLatest() {
        DecodedVideoFrame frame = VideoFrameStore.takeLatest(sessionId);
        if (frame == null || frame.presentationTimeMicros() <= presentationTimeMicros) {
            return false;
        }

        ensureSize(frame.width(), frame.height());
        copyRgba(frame.rgba(), pixels, frame.width(), frame.height());

        RenderSystem.getDevice().createCommandEncoder().writeToTexture(
            texture.getTexture(),
            pixels,
            0,
            0,
            0,
            0
        );

        presentationTimeMicros = frame.presentationTimeMicros();
        return true;
    }

    private void ensureSize(int newWidth, int newHeight) {
        if (texture != null && width == newWidth && height == newHeight) {
            return;
        }

        releaseTexture();

        width = newWidth;
        height = newHeight;
        pixels = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        texture = new DynamicTexture(
            () -> "Caszual TV Time stream " + sessionId,
            pixels
        );

        Minecraft.getInstance().getTextureManager().register(textureId, texture);
    }

    private static void copyRgba(ByteBuffer source, NativeImage destination, int width, int height) {
        ByteBuffer src = source.duplicate();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (y * width + x) * 4;
                int r = src.get(offset) & 0xff;
                int g = src.get(offset + 1) & 0xff;
                int b = src.get(offset + 2) & 0xff;
                int a = src.get(offset + 3) & 0xff;

                destination.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    private void releaseTexture() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(textureId);
            texture = null;
            pixels = null;
        }
    }

    @Override
    public void close() {
        releaseTexture();
        width = 0;
        height = 0;
        presentationTimeMicros = Long.MIN_VALUE;
    }
}
