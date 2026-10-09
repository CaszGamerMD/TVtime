package com.caszgamermd.caszualtvtime.client.media;

import com.caszgamermd.caszualtvtime.client.capture.CapturedAudioChunk;

import java.nio.ByteBuffer;

public final class RawPcmAudioCodec {
    private static final int MAGIC = 0x54564130; // TVA0
    private static final int HEADER_BYTES = 12;

    private RawPcmAudioCodec() {
    }

    public static byte[] encode(CapturedAudioChunk chunk) {
        if (chunk.format() != CapturedAudioChunk.SampleFormat.S16_LE) {
            throw new IllegalArgumentException("Raw audio relay currently requires S16_LE");
        }

        ByteBuffer samples = chunk.samples().duplicate();
        ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + samples.remaining());

        out.putInt(MAGIC);
        out.putInt(chunk.sampleRate());
        out.putInt(chunk.channels());
        out.put(samples);
        return out.array();
    }

    public static DecodedAudioChunk decode(
        byte[] packet,
        long presentationTimeMicros
    ) {
        if (packet == null || packet.length < HEADER_BYTES) {
            return null;
        }

        ByteBuffer in = ByteBuffer.wrap(packet);
        if (in.getInt() != MAGIC) {
            return null;
        }

        int sampleRate = in.getInt();
        int channels = in.getInt();

        if (sampleRate <= 0 || channels <= 0 || (in.remaining() & 1) != 0) {
            return null;
        }

        ByteBuffer samples = ByteBuffer.allocateDirect(in.remaining());
        samples.put(in);
        samples.flip();

        return new DecodedAudioChunk(
            sampleRate,
            channels,
            presentationTimeMicros,
            samples
        );
    }
}
