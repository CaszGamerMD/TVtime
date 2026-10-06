package com.caszgamermd.tvtime.client.audio;

import com.caszgamermd.tvtime.audio.SpeakerChannel;
import com.caszgamermd.tvtime.client.media.DecodedAudioChunk;
import com.caszgamermd.tvtime.client.media.VideoFrameStore;
import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public final class TvPcmAudioStream implements AudioStream {
    private static final int SAMPLE_RATE = 48_000;
    private static final int OUTPUT_CHANNELS = 1;
    private static final int BYTES_PER_SAMPLE = 2;

    // Minecraft's Channel asks AudioStream for one full second (96 KB mono)
    // and queues four reads. Returning a single 20 ms packet caused frequent
    // under-runs. We intentionally return 40 ms live buffers instead:
    // four queued buffers ~= 160 ms of jitter protection, while keeping
    // latency low enough for streamed video.
    private static final int TARGET_BUFFER_MILLIS = 40;
    private static final int TARGET_BUFFER_BYTES =
        SAMPLE_RATE
            * OUTPUT_CHANNELS
            * BYTES_PER_SAMPLE
            * TARGET_BUFFER_MILLIS
            / 1000;

    private static final int FIRST_PACKET_WAIT_MILLIS = 80;
    private static final int NEXT_PACKET_WAIT_MILLIS = 24;
    private static final long MAX_AUDIO_BEHIND_VIDEO_MICROS = 120_000L;

    private final UUID sessionId;
    private final SpeakerChannel speakerChannel;
    private final TvAudioBus.Reader reader;
    private ByteBuffer pendingMono = ByteBuffer.allocateDirect(0);

    private final AudioFormat format =
        new AudioFormat(
            SAMPLE_RATE,
            16,
            OUTPUT_CHANNELS,
            true,
            false
        );

    private volatile boolean closed;

    public TvPcmAudioStream(
        UUID sessionId,
        SpeakerChannel speakerChannel
    ) {
        this.sessionId = sessionId;
        this.speakerChannel = speakerChannel == null
            ? SpeakerChannel.FULL
            : speakerChannel;
        this.reader = TvAudioBus.subscribe(sessionId);
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int expectedSize) throws IOException {
        if (closed) {
            return null;
        }

        ByteBuffer output = ByteBuffer
            .allocateDirect(TARGET_BUFFER_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN);

        boolean receivedAudio = false;

        if (pendingMono.hasRemaining()) {
            copyInto(output, pendingMono);
            receivedAudio = true;
        }

        while (output.hasRemaining() && !closed) {
            long waitMillis = receivedAudio
                ? NEXT_PACKET_WAIT_MILLIS
                : FIRST_PACKET_WAIT_MILLIS;

            DecodedAudioChunk chunk =
                reader.pollWaiting(waitMillis);

            if (chunk == null) {
                break;
            }

            long videoPts =
                VideoFrameStore.latestPresentationTimeMicros(sessionId);

            while (videoPts != Long.MIN_VALUE
                && chunk.presentationTimeMicros()
                    < videoPts - MAX_AUDIO_BEHIND_VIDEO_MICROS) {
                DecodedAudioChunk newer = reader.poll();
                if (newer == null) {
                    chunk = null;
                    break;
                }
                chunk = newer;
            }

            if (chunk == null) {
                break;
            }

            if (chunk.sampleRate() != SAMPLE_RATE) {
                continue;
            }

            ByteBuffer mono = toMono(chunk);
            copyInto(output, mono);

            if (mono.hasRemaining()) {
                ByteBuffer remainder =
                    ByteBuffer.allocateDirect(mono.remaining());
                remainder.put(mono);
                remainder.flip();
                pendingMono = remainder;
            }

            receivedAudio = true;
        }

        if (!receivedAudio) {
            // Preserve stream continuity if the network misses the jitter
            // window. This is intentionally only 40 ms, not Minecraft's
            // requested one-second buffer.
            while (output.hasRemaining()) {
                output.put((byte) 0);
            }
        }

        output.flip();
        return output;
    }

    private static void copyInto(
        ByteBuffer destination,
        ByteBuffer source
    ) {
        int copy = Math.min(
            destination.remaining(),
            source.remaining()
        );

        int oldLimit = source.limit();
        source.limit(source.position() + copy);
        destination.put(source);
        source.limit(oldLimit);
    }

    private ByteBuffer toMono(DecodedAudioChunk chunk) {
        ByteBuffer source = chunk.samples()
            .duplicate()
            .order(ByteOrder.LITTLE_ENDIAN);

        int channels = chunk.channels();
        if (channels <= 0) {
            return ByteBuffer.allocateDirect(0);
        }

        int frameBytes = channels * BYTES_PER_SAMPLE;
        int frames = source.remaining() / frameBytes;

        ByteBuffer mono = ByteBuffer
            .allocateDirect(frames * BYTES_PER_SAMPLE)
            .order(ByteOrder.LITTLE_ENDIAN);

        for (int frame = 0; frame < frames; frame++) {
            int base = source.position() + frame * frameBytes;

            short left = source.getShort(base);
            short right = channels >= 2
                ? source.getShort(base + BYTES_PER_SAMPLE)
                : left;

            int sample = switch (speakerChannel) {
                case LEFT, REAR_LEFT -> left;
                case RIGHT, REAR_RIGHT -> right;
                case FULL, CENTER, LFE -> (left + right) / 2;
            };

            mono.putShort((short) sample);
        }

        mono.flip();
        return mono;
    }

    @Override
    public void close() {
        closed = true;
        pendingMono = ByteBuffer.allocateDirect(0);
        reader.close();
    }
}
