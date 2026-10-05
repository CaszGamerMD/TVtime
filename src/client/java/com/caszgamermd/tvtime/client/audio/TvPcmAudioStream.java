package com.caszgamermd.tvtime.client.audio;

import com.caszgamermd.tvtime.client.media.AudioChunkStore;
import com.caszgamermd.tvtime.client.media.DecodedAudioChunk;
import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class TvPcmAudioStream implements AudioStream {
    private static final int SAMPLE_RATE = 44_100;
    private static final int CHANNELS = 2;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int SILENCE_MILLIS = 20;
    private static final int SILENCE_BYTES =
        SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * SILENCE_MILLIS / 1000;

    private final UUID sessionId;
    private final AudioFormat format =
        new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);

    private volatile boolean closed;

    public TvPcmAudioStream(UUID sessionId) {
        this.sessionId = sessionId;
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

        DecodedAudioChunk chunk = AudioChunkStore.poll(sessionId);
        if (chunk == null) {
            return ByteBuffer.allocateDirect(SILENCE_BYTES);
        }

        if (chunk.sampleRate() != SAMPLE_RATE || chunk.channels() != CHANNELS) {
            return ByteBuffer.allocateDirect(SILENCE_BYTES);
        }

        ByteBuffer samples = chunk.samples().duplicate();
        ByteBuffer copy = ByteBuffer.allocateDirect(samples.remaining());
        copy.put(samples);
        copy.flip();
        return copy;
    }

    @Override
    public void close() {
        closed = true;
    }
}
