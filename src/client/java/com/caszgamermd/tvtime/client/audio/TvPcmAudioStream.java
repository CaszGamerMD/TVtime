package com.caszgamermd.tvtime.client.audio;

import com.caszgamermd.tvtime.audio.SpeakerChannel;
import com.caszgamermd.tvtime.client.media.DecodedAudioChunk;
import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public final class TvPcmAudioStream implements AudioStream {
    private static final int SAMPLE_RATE = 44_100;
    private static final int OUTPUT_CHANNELS = 1;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int SILENCE_MILLIS = 20;
    private static final int SILENCE_BYTES =
        SAMPLE_RATE
            * OUTPUT_CHANNELS
            * BYTES_PER_SAMPLE
            * SILENCE_MILLIS
            / 1000;

    private final UUID sessionId;
    private final SpeakerChannel speakerChannel;
    private final TvAudioBus.Reader reader;
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

        DecodedAudioChunk chunk = reader.poll();
        if (chunk == null) {
            return ByteBuffer.allocateDirect(SILENCE_BYTES);
        }

        if (chunk.sampleRate() != SAMPLE_RATE) {
            return ByteBuffer.allocateDirect(SILENCE_BYTES);
        }

        return toMono(chunk);
    }

    private ByteBuffer toMono(DecodedAudioChunk chunk) {
        ByteBuffer source = chunk.samples()
            .duplicate()
            .order(ByteOrder.LITTLE_ENDIAN);

        int channels = chunk.channels();
        if (channels <= 0) {
            return ByteBuffer.allocateDirect(SILENCE_BYTES);
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
        reader.close();
    }
}
