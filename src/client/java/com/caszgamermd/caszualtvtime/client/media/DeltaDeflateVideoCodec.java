package com.caszgamermd.caszualtvtime.client.media;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Lightweight inter-frame codec for the development stream.
 *
 * Keyframes Deflate the full RGBA image. Delta frames XOR the current frame
 * against the previous frame, then Deflate the difference. Desktop/window
 * content typically changes only small regions, so this is substantially
 * smaller than independently compressing every frame.
 */
public final class DeltaDeflateVideoCodec {
    private static final int MAGIC = 0x54564431; // TVD1
    private static final int HEADER_BYTES = 17;
    private static final int FLAG_KEYFRAME = 1;
    private static final int MAX_DECODED_BYTES = 16 * 1024 * 1024;

    private DeltaDeflateVideoCodec() {
    }

    public static Encoded encode(
        int width,
        int height,
        ByteBuffer rgba,
        byte[] previous,
        boolean forceKeyFrame
    ) {
        int pixelBytes = Math.multiplyExact(
            Math.multiplyExact(width, height),
            4
        );

        if (rgba.remaining() != pixelBytes) {
            throw new IllegalArgumentException(
                "Unexpected RGBA buffer length"
            );
        }

        byte[] current = new byte[pixelBytes];
        rgba.duplicate().get(current);

        boolean keyFrame =
            forceKeyFrame
                || previous == null
                || previous.length != current.length;

        byte[] source;
        if (keyFrame) {
            source = current;
        } else {
            source = new byte[current.length];
            for (int i = 0; i < current.length; i++) {
                source[i] = (byte) (current[i] ^ previous[i]);
            }
        }

        byte[] compressed = deflate(source);

        ByteBuffer out = ByteBuffer.allocate(
            HEADER_BYTES + compressed.length
        );

        out.putInt(MAGIC);
        out.put((byte) (keyFrame ? FLAG_KEYFRAME : 0));
        out.putInt(width);
        out.putInt(height);
        out.putInt(pixelBytes);
        out.put(compressed);

        return new Encoded(
            out.array(),
            current,
            keyFrame
        );
    }

    public static Decoded decode(
        byte[] packet,
        long presentationTimeMicros,
        byte[] previous
    ) {
        if (packet == null || packet.length < HEADER_BYTES) {
            return null;
        }

        ByteBuffer in = ByteBuffer.wrap(packet);
        if (in.getInt() != MAGIC) {
            return null;
        }

        boolean keyFrame = (in.get() & FLAG_KEYFRAME) != 0;
        int width = in.getInt();
        int height = in.getInt();
        int decodedBytes = in.getInt();

        if (width <= 0 || height <= 0 || decodedBytes <= 0) {
            return null;
        }

        long expected = (long) width * height * 4L;
        if (expected != decodedBytes
            || decodedBytes > MAX_DECODED_BYTES) {
            return null;
        }

        if (!keyFrame
            && (previous == null || previous.length != decodedBytes)) {
            return new Decoded(null, null, false, true);
        }

        byte[] compressed = new byte[in.remaining()];
        in.get(compressed);

        byte[] decoded = inflate(compressed, decodedBytes);
        if (decoded == null) {
            return null;
        }

        byte[] current;
        if (keyFrame) {
            current = decoded;
        } else {
            current = Arrays.copyOf(previous, previous.length);
            for (int i = 0; i < current.length; i++) {
                current[i] = (byte) (current[i] ^ decoded[i]);
            }
        }

        ByteBuffer pixels = ByteBuffer.allocateDirect(current.length);
        pixels.put(current);
        pixels.flip();

        return new Decoded(
            new DecodedVideoFrame(
                width,
                height,
                presentationTimeMicros,
                pixels
            ),
            current,
            keyFrame,
            false
        );
    }

    private static byte[] deflate(byte[] source) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(source);
        deflater.finish();

        ByteArrayOutputStream out =
            new ByteArrayOutputStream(Math.max(1024, source.length / 8));

        byte[] buffer = new byte[16 * 1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            if (count <= 0) {
                break;
            }
            out.write(buffer, 0, count);
        }

        deflater.end();
        return out.toByteArray();
    }

    private static byte[] inflate(
        byte[] compressed,
        int expectedBytes
    ) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);

        byte[] decoded = new byte[expectedBytes];

        try {
            int offset = 0;

            while (!inflater.finished() && offset < decoded.length) {
                int count = inflater.inflate(
                    decoded,
                    offset,
                    decoded.length - offset
                );

                if (count == 0) {
                    if (inflater.needsInput()
                        || inflater.needsDictionary()) {
                        break;
                    }
                } else {
                    offset += count;
                }
            }

            if (!inflater.finished() || offset != decoded.length) {
                return null;
            }

            return decoded;
        } catch (DataFormatException invalid) {
            return null;
        } finally {
            inflater.end();
        }
    }

    public record Encoded(
        byte[] payload,
        byte[] currentFrame,
        boolean keyFrame
    ) {
    }

    public record Decoded(
        DecodedVideoFrame frame,
        byte[] currentFrame,
        boolean keyFrame,
        boolean needsKeyFrame
    ) {
    }
}
