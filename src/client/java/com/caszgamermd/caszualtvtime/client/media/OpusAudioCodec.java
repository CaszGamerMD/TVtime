package com.caszgamermd.caszualcaszual_tv_time.client.media;

import com.caszgamermd.caszualcaszual_tv_time.client.capture.CapturedAudioChunk;
import org.concentus.OpusApplication;
import org.concentus.OpusDecoder;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OpusAudioCodec {
    private static final int MAGIC = 0x54564f31; // TVO1
    private static final int HEADER_BYTES = 16;
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHANNELS = 2;
    private static final int FRAME_SAMPLES = 960; // 20 ms @ 48 kHz
    private static final int FRAME_BYTES =
        FRAME_SAMPLES * CHANNELS * 2;
    private static final int MAX_PACKET_BYTES = 1275;
    private static final long FRAME_MICROS = 20_000L;

    private OpusAudioCodec() {
    }

    public static boolean isOpusPacket(byte[] packet) {
        return packet != null
            && packet.length >= 4
            && ByteBuffer.wrap(packet).getInt() == MAGIC;
    }

    public static final class Encoder {
        private final OpusEncoder encoder;
        private final ByteArrayOutputStream pending =
            new ByteArrayOutputStream(FRAME_BYTES * 2);

        private long nextTimestampMicros = Long.MIN_VALUE;

        public Encoder() {
            try {
                encoder = new OpusEncoder(
                    SAMPLE_RATE,
                    CHANNELS,
                    OpusApplication.OPUS_APPLICATION_AUDIO
                );
                encoder.setBitrate(128_000);
                encoder.setComplexity(7);
            } catch (OpusException ex) {
                throw new IllegalStateException(
                    "Unable to initialize Opus encoder",
                    ex
                );
            }
        }

        public synchronized List<EncodedPacket> encode(
            CapturedAudioChunk chunk
        ) {
            if (chunk.format() != CapturedAudioChunk.SampleFormat.S16_LE
                || chunk.sampleRate() != SAMPLE_RATE
                || chunk.channels() != CHANNELS) {
                throw new IllegalArgumentException(
                    "Opus relay requires 48 kHz stereo S16_LE"
                );
            }

            ByteBuffer source = chunk.samples().duplicate();
            byte[] incoming = new byte[source.remaining()];
            source.get(incoming);

            if (pending.size() == 0) {
                nextTimestampMicros = chunk.timestampMicros();
            }

            pending.writeBytes(incoming);

            byte[] all = pending.toByteArray();
            int offset = 0;
            List<EncodedPacket> packets = new ArrayList<>();

            while (all.length - offset >= FRAME_BYTES) {
                byte[] opus = new byte[MAX_PACKET_BYTES];

                final int encodedBytes;
                try {
                    encodedBytes = encoder.encode(
                        all,
                        offset,
                        FRAME_SAMPLES,
                        opus,
                        0,
                        opus.length
                    );
                } catch (OpusException ex) {
                    throw new IllegalStateException(
                        "Opus encoding failed",
                        ex
                    );
                }

                ByteBuffer framed = ByteBuffer.allocate(
                    HEADER_BYTES + encodedBytes
                );

                framed.putInt(MAGIC);
                framed.putInt(SAMPLE_RATE);
                framed.putInt(CHANNELS);
                framed.putInt(FRAME_SAMPLES);
                framed.put(opus, 0, encodedBytes);

                packets.add(new EncodedPacket(
                    framed.array(),
                    nextTimestampMicros
                ));

                nextTimestampMicros += FRAME_MICROS;
                offset += FRAME_BYTES;
            }

            pending.reset();
            if (offset < all.length) {
                pending.writeBytes(
                    Arrays.copyOfRange(all, offset, all.length)
                );
            }

            return List.copyOf(packets);
        }

        public synchronized void reset() {
            pending.reset();
            nextTimestampMicros = Long.MIN_VALUE;
            encoder.resetState();
        }
    }

    public static final class Decoder {
        private OpusDecoder decoder;

        public Decoder() {
            decoder = createDecoder();
        }

        public synchronized DecodedAudioChunk decode(
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
            int frameSamples = in.getInt();

            if (sampleRate != SAMPLE_RATE
                || channels != CHANNELS
                || frameSamples != FRAME_SAMPLES) {
                return null;
            }

            byte[] opus = new byte[in.remaining()];
            in.get(opus);

            short[] pcm = new short[FRAME_SAMPLES * CHANNELS];

            final int decodedSamples;
            try {
                decodedSamples = decoder.decode(
                    opus,
                    0,
                    opus.length,
                    pcm,
                    0,
                    FRAME_SAMPLES,
                    false
                );
            } catch (OpusException ex) {
                reset();
                return null;
            }

            if (decodedSamples <= 0) {
                return null;
            }

            ByteBuffer samples = ByteBuffer
                .allocateDirect(decodedSamples * CHANNELS * 2)
                .order(ByteOrder.LITTLE_ENDIAN);

            int count = decodedSamples * CHANNELS;
            for (int i = 0; i < count; i++) {
                samples.putShort(pcm[i]);
            }
            samples.flip();

            return new DecodedAudioChunk(
                SAMPLE_RATE,
                CHANNELS,
                presentationTimeMicros,
                samples
            );
        }

        public synchronized DecodedAudioChunk concealLoss(
            long presentationTimeMicros
        ) {
            short[] pcm = new short[FRAME_SAMPLES * CHANNELS];

            final int decodedSamples;
            try {
                decodedSamples = decoder.decode(
                    null,
                    0,
                    0,
                    pcm,
                    0,
                    FRAME_SAMPLES,
                    false
                );
            } catch (OpusException ex) {
                reset();
                return null;
            }

            if (decodedSamples <= 0) {
                return null;
            }

            ByteBuffer samples = ByteBuffer
                .allocateDirect(decodedSamples * CHANNELS * 2)
                .order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < decodedSamples * CHANNELS; i++) {
                samples.putShort(pcm[i]);
            }
            samples.flip();

            return new DecodedAudioChunk(
                SAMPLE_RATE,
                CHANNELS,
                presentationTimeMicros,
                samples
            );
        }

        public synchronized void reset() {
            decoder = createDecoder();
        }

        private static OpusDecoder createDecoder() {
            try {
                return new OpusDecoder(SAMPLE_RATE, CHANNELS);
            } catch (OpusException ex) {
                throw new IllegalStateException(
                    "Unable to initialize Opus decoder",
                    ex
                );
            }
        }
    }

    public record EncodedPacket(
        byte[] payload,
        long presentationTimeMicros
    ) {
    }
}
