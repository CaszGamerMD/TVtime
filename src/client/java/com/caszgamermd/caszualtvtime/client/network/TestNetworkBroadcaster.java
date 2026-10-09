package com.caszgamermd.caszualcaszual_tv_time.client.network;

import com.caszgamermd.caszualcaszual_tv_time.client.media.RawTestFrameCodec;
import com.caszgamermd.caszualcaszual_tv_time.network.MediaKind;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.MediaRelayPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.StartBroadcastPayload;
import com.caszgamermd.caszualcaszual_tv_time.network.payload.StopBroadcastPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class TestNetworkBroadcaster {
    private static final int WIDTH = 96;
    private static final int HEIGHT = 54;
    private static final int FPS = 10;
    private static final long FRAME_INTERVAL_NANOS = 1_000_000_000L / FPS;

    private static String pendingChannel;
    private static long lastFrameNanos;
    private static long sequence;

    private TestNetworkBroadcaster() {
    }

    public static boolean start(String channel) {
        String normalized = channel == null ? "" : channel.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        if (!ClientPlayNetworking.canSend(StartBroadcastPayload.TYPE)) {
            return false;
        }

        pendingChannel = normalized;
        ClientBroadcastState.clear();
        ClientPlayNetworking.send(
            new StartBroadcastPayload(normalized, WIDTH, HEIGHT, FPS, 512)
        );
        return true;
    }

    public static void stop() {
        ClientBroadcastState.ActiveBroadcast active = ClientBroadcastState.active();
        if (active != null && ClientPlayNetworking.canSend(StopBroadcastPayload.TYPE)) {
            ClientPlayNetworking.send(new StopBroadcastPayload(active.sessionId()));
        }

        pendingChannel = null;
        ClientBroadcastState.clear();
        sequence = 0;
        lastFrameNanos = 0;
    }

    public static void tick() {
        ClientBroadcastState.ActiveBroadcast active = ClientBroadcastState.active();
        if (active == null || pendingChannel == null) {
            return;
        }
        if (!pendingChannel.equals(active.channel())) {
            return;
        }
        if (!ClientPlayNetworking.canSend(MediaRelayPayload.TYPE)) {
            return;
        }

        long now = System.nanoTime();
        if (now - lastFrameNanos < FRAME_INTERVAL_NANOS) {
            return;
        }
        lastFrameNanos = now;

        ByteBuffer rgba = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4);
        int sweep = (int) ((sequence * 4) % WIDTH);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int r = x * 255 / Math.max(1, WIDTH - 1);
                int g = y * 255 / Math.max(1, HEIGHT - 1);
                int b = 64;

                if (Math.abs(x - sweep) <= 1) {
                    r = 255;
                    g = 255;
                    b = 255;
                }

                rgba.put((byte) r);
                rgba.put((byte) g);
                rgba.put((byte) b);
                rgba.put((byte) 255);
            }
        }

        rgba.flip();
        byte[] packet = RawTestFrameCodec.encode(WIDTH, HEIGHT, rgba);

        ClientPlayNetworking.send(new MediaRelayPayload(
            active.sessionId(),
            MediaKind.VIDEO,
            sequence++,
            now / 1_000L,
            true,
            packet
        ));
    }

    public static String pendingChannel() {
        return pendingChannel;
    }
}
