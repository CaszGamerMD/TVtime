package com.caszgamermd.tvtime.client.capture;

import com.caszgamermd.tvtime.client.media.RawTestFrameCodec;
import com.caszgamermd.tvtime.client.media.DecodedVideoFrame;
import com.caszgamermd.tvtime.client.media.VideoFrameStore;
import com.caszgamermd.tvtime.client.network.ClientBroadcastState;
import com.caszgamermd.tvtime.network.MediaKind;
import com.caszgamermd.tvtime.network.payload.MediaRelayPayload;
import com.caszgamermd.tvtime.network.payload.StartBroadcastPayload;
import com.caszgamermd.tvtime.network.payload.StopBroadcastPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class CaptureBroadcastController {
    private static final CaptureBroadcastController INSTANCE =
        new CaptureBroadcastController();

    private final AtomicLong sequence = new AtomicLong();

    private WindowCaptureBackend backend;
    private String requestedChannel;

    private CaptureBroadcastController() {
    }

    public static CaptureBroadcastController instance() {
        return INSTANCE;
    }

    public synchronized WindowCaptureBackend backend() {
        if (backend == null) {
            backend = CaptureBackends.createDefault();
        }
        return backend;
    }

    public synchronized List<CaptureWindow> windows() {
        return backend().listWindows();
    }

    public synchronized void start(int windowIndex, String channel) {
        List<CaptureWindow> windows = windows();
        if (windowIndex < 0 || windowIndex >= windows.size()) {
            throw new IllegalArgumentException(
                "Window index must be between 0 and " + Math.max(0, windows.size() - 1)
            );
        }

        String normalized = channel == null ? "" : channel.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Channel cannot be blank");
        }

        stop();
        requestedChannel = normalized;
        sequence.set(0);

        if (!ClientPlayNetworking.canSend(StartBroadcastPayload.TYPE)) {
            requestedChannel = null;
            throw new IllegalStateException("Server does not support TVtime broadcasts");
        }

        ClientPlayNetworking.send(
            new StartBroadcastPayload(normalized, 128, 72, 10, 4000)
        );

        backend().start(
            windows.get(windowIndex),
            new CaptureOptions(128, 72, 10, false),
            new WindowCaptureBackend.Listener() {
                @Override
                public void onVideoFrame(CapturedVideoFrame frame) {
                    handleFrame(frame);
                }

                @Override
                public void onAudioChunk(CapturedAudioChunk chunk) {
                    // Audio is added once the process-loopback path is wired.
                }

                @Override
                public void onCaptureStopped(String reason) {
                    Minecraft.getInstance().execute(() -> {
                        if (requestedChannel != null) {
                            requestedChannel = null;
                            ClientBroadcastState.ActiveBroadcast active =
                                ClientBroadcastState.active();
                            if (active != null
                                && ClientPlayNetworking.canSend(StopBroadcastPayload.TYPE)) {
                                ClientPlayNetworking.send(
                                    new StopBroadcastPayload(active.sessionId())
                                );
                            }
                            ClientBroadcastState.clear();
                        }
                    });
                }
            }
        );
    }

    public synchronized void stop() {
        if (backend != null && backend.running()) {
            backend.stop();
        }

        ClientBroadcastState.ActiveBroadcast active = ClientBroadcastState.active();
        if (active != null && ClientPlayNetworking.canSend(StopBroadcastPayload.TYPE)) {
            ClientPlayNetworking.send(new StopBroadcastPayload(active.sessionId()));
        }

        requestedChannel = null;
        sequence.set(0);
        ClientBroadcastState.clear();
    }

    public synchronized boolean running() {
        return requestedChannel != null && backend != null && backend.running();
    }

    public synchronized String channel() {
        return requestedChannel;
    }

    public synchronized void close() {
        stop();
        if (backend != null) {
            backend.close();
            backend = null;
        }
    }

    private void handleFrame(CapturedVideoFrame frame) {
        String expectedChannel = requestedChannel;
        if (expectedChannel == null) {
            return;
        }

        ClientBroadcastState.ActiveBroadcast active = ClientBroadcastState.active();
        if (active == null || !expectedChannel.equals(active.channel())) {
            return;
        }

        ByteBuffer rgba = toRgba(frame);
        VideoFrameStore.publish(
            active.sessionId(),
            new DecodedVideoFrame(
                frame.width(),
                frame.height(),
                frame.timestampMicros(),
                rgba.duplicate()
            )
        );

        byte[] encoded = RawTestFrameCodec.encode(
            frame.width(),
            frame.height(),
            rgba
        );

        if (encoded.length > MediaRelayPayload.MAX_MEDIA_BYTES) {
            return;
        }

        long packetSequence = sequence.getAndIncrement();
        Minecraft.getInstance().execute(() -> {
            ClientBroadcastState.ActiveBroadcast current =
                ClientBroadcastState.active();

            if (current == null
                || !current.sessionId().equals(active.sessionId())
                || !ClientPlayNetworking.canSend(MediaRelayPayload.TYPE)) {
                return;
            }

            ClientPlayNetworking.send(new MediaRelayPayload(
                current.sessionId(),
                MediaKind.VIDEO,
                packetSequence,
                frame.timestampMicros(),
                true,
                encoded
            ));
        });
    }

    private static ByteBuffer toRgba(CapturedVideoFrame frame) {
        ByteBuffer source = frame.pixels().duplicate();
        int bytes = Math.multiplyExact(
            Math.multiplyExact(frame.width(), frame.height()),
            4
        );

        if (source.remaining() != bytes) {
            throw new IllegalArgumentException("Captured frame has unexpected byte count");
        }

        if (frame.format() == CapturedVideoFrame.PixelFormat.RGBA8) {
            ByteBuffer copy = ByteBuffer.allocateDirect(bytes);
            copy.put(source);
            copy.flip();
            return copy;
        }

        ByteBuffer rgba = ByteBuffer.allocateDirect(bytes);
        while (source.remaining() >= 4) {
            byte b = source.get();
            byte g = source.get();
            byte r = source.get();
            byte a = source.get();

            rgba.put(r);
            rgba.put(g);
            rgba.put(b);
            rgba.put(a);
        }
        rgba.flip();
        return rgba;
    }
}
