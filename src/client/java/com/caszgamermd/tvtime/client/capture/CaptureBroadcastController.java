package com.caszgamermd.tvtime.client.capture;

import com.caszgamermd.tvtime.client.media.RawTestFrameCodec;
import com.caszgamermd.tvtime.client.media.DecodedVideoFrame;
import com.caszgamermd.tvtime.client.media.VideoFrameStore;
import com.caszgamermd.tvtime.client.media.RawPcmAudioCodec;
import com.caszgamermd.tvtime.client.media.AudioChunkStore;
import com.caszgamermd.tvtime.client.media.DecodedAudioChunk;
import com.caszgamermd.tvtime.client.media.MediaFragmenter;
import com.caszgamermd.tvtime.client.media.DeflateVideoCodec;
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
    private final AtomicLong videoFrames = new AtomicLong();
    private final AtomicLong rawVideoBytes = new AtomicLong();
    private final AtomicLong encodedVideoBytes = new AtomicLong();
    private final AtomicLong audioBytes = new AtomicLong();
    private volatile long startedAtMillis;

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
        videoFrames.set(0);
        rawVideoBytes.set(0);
        encodedVideoBytes.set(0);
        audioBytes.set(0);
        startedAtMillis = System.currentTimeMillis();

        if (!ClientPlayNetworking.canSend(StartBroadcastPayload.TYPE)) {
            requestedChannel = null;
            throw new IllegalStateException("Server does not support TVtime broadcasts");
        }

        ClientPlayNetworking.send(
            new StartBroadcastPayload(normalized, 128, 72, 10, 4000)
        );

        try {
            backend().start(
                windows.get(windowIndex),
                new CaptureOptions(128, 72, 10, true),
                new WindowCaptureBackend.Listener() {
                    @Override
                    public void onVideoFrame(CapturedVideoFrame frame) {
                        handleFrame(frame);
                    }

                    @Override
                    public void onAudioChunk(CapturedAudioChunk chunk) {
                        handleAudio(chunk);
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
        } catch (Exception ex) {
            requestedChannel = null;
            ClientBroadcastState.clear();
            throw new IllegalStateException("Unable to start window capture", ex);
        }
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
        startedAtMillis = 0;
        ClientBroadcastState.clear();
    }

    public synchronized boolean running() {
        return requestedChannel != null && backend != null && backend.running();
    }

    public synchronized String channel() {
        return requestedChannel;
    }

    public Stats stats() {
        long started = startedAtMillis;
        long elapsedMillis = started == 0
            ? 0
            : Math.max(1, System.currentTimeMillis() - started);

        long raw = rawVideoBytes.get();
        long encoded = encodedVideoBytes.get();
        long audio = audioBytes.get();
        long frames = videoFrames.get();

        double seconds = elapsedMillis / 1000.0;
        double fps = seconds <= 0 ? 0.0 : frames / seconds;
        double kbps = seconds <= 0
            ? 0.0
            : ((encoded + audio) * 8.0 / 1000.0) / seconds;
        double compression = raw <= 0
            ? 1.0
            : encoded / (double) raw;

        return new Stats(
            frames,
            raw,
            encoded,
            audio,
            fps,
            kbps,
            compression
        );
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

        byte[] encoded = DeflateVideoCodec.encode(
            frame.width(),
            frame.height(),
            rgba
        );

        videoFrames.incrementAndGet();
        rawVideoBytes.addAndGet(rgba.remaining());
        encodedVideoBytes.addAndGet(encoded.length);

        long packetSequence = sequence.getAndIncrement();
        sendEncoded(
            active,
            MediaKind.VIDEO,
            packetSequence,
            frame.timestampMicros(),
            true,
            encoded
        );
    }

    private void handleAudio(CapturedAudioChunk chunk) {
        String expectedChannel = requestedChannel;
        if (expectedChannel == null) {
            return;
        }

        ClientBroadcastState.ActiveBroadcast active = ClientBroadcastState.active();
        if (active == null || !expectedChannel.equals(active.channel())) {
            return;
        }

        byte[] encoded = RawPcmAudioCodec.encode(chunk);
        audioBytes.addAndGet(encoded.length);
        AudioChunkStore.offer(
            active.sessionId(),
            new DecodedAudioChunk(
                chunk.sampleRate(),
                chunk.channels(),
                chunk.timestampMicros(),
                chunk.samples().duplicate()
            )
        );

        long packetSequence = sequence.getAndIncrement();
        sendEncoded(
            active,
            MediaKind.AUDIO,
            packetSequence,
            chunk.timestampMicros(),
            false,
            encoded
        );
    }

    private void sendEncoded(
        ClientBroadcastState.ActiveBroadcast active,
        MediaKind kind,
        long packetSequence,
        long presentationTimeMicros,
        boolean keyFrame,
        byte[] encoded
    ) {
        final var fragments = MediaFragmenter.fragment(
            active.sessionId(),
            kind,
            packetSequence,
            presentationTimeMicros,
            keyFrame,
            encoded
        );

        Minecraft.getInstance().execute(() -> {
            ClientBroadcastState.ActiveBroadcast current =
                ClientBroadcastState.active();

            if (current == null
                || !current.sessionId().equals(active.sessionId())) {
                return;
            }

            for (var fragment : fragments) {
                if (!ClientPlayNetworking.canSend(fragment.type())) {
                    return;
                }
                ClientPlayNetworking.send(fragment);
            }
        });
    }

    public record Stats(
        long videoFrames,
        long rawVideoBytes,
        long encodedVideoBytes,
        long audioBytes,
        double fps,
        double kbps,
        double compressionRatio
    ) {
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
