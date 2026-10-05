package com.caszgamermd.tvtime.client.audio;

import com.caszgamermd.tvtime.audio.SpeakerChannel;
import com.caszgamermd.tvtime.client.media.AudioChunkStore;
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
        SAMPLE_RATE * OUTPUT_CHANNELS * BYTES_PER_SAMPLE * SILENCE_MILLIS / 1000;

    private final UUID sessionId;
    private final SpeakerChannel speakerChannel;
    private final AudioFormat format =
        new AudioFormat(SAMPLE_RATE, 16, OUTPUT_CHANNELS, true, false);

    private volatile boolean closed;

    public TvPcmAudioStream(UUID sessionId, SpeakerChannel speakerChannel) {
        this.sessionId = sessionId;
        this.speakerChannel = speakerChannel == null
            ? SpeakerChannel.FULL
            : speakerChannel;
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
            return silence();
        }

        if (chunk.sampleRate() != SAMPLE_RATE) {
            return silence();
        }

        ByteBuffer source = chunk.samples()
            .duplicate()
            .order(ByteOrder.LITTLE_ENDIAN);

        if (chunk.channels() == 1) {
            ByteBuffer mono = ByteBuffer.allocateDirect(source.remaining())
                .order(ByteOrder.LITTLE_ENDIAN);
            mono.put(source);
            mono.flip();
            return mono;
        }

        if (chunk.channels() != 2 || (source.remaining() % 4) != 0) {
            return silence();
        }

        int frames = source.remaining() / 4;
        ByteBuffer mono = ByteBuffer.allocateDirect(frames * 2)
            .order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < frames; i++) {
            short left = source.getShort();
            short right = source.getShort();

            int sample = switch (speakerChannel) {
                case LEFT, REAR_LEFT -> left;
                case RIGHT, REAR_RIGHT -> right;
                case FULL, CENTER, LFE -> ((int) left + (int) right) / 2;
            };

            mono.putShort((short) sample);
        }

        mono.flip();
        return mono;
    }

    @Override
    public void close() {
        closed = true;
    }

    private static ByteBuffer silence() {
        return ByteBuffer.allocateDirect(SILENCE_BYTES);
    }
}
