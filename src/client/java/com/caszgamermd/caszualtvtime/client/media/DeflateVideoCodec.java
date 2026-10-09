package com.caszgamermd.caszualcaszual_tv_time.client.media;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class DeflateVideoCodec {
    private static final int MAGIC = 0x54565a31; // TVZ1
    private static final int HEADER_BYTES = 16;
    private static final int MAX_DECODED_BYTES = 16 * 1024 * 1024;

    private DeflateVideoCodec() {
    }

    public static byte[] encode(int width, int height, ByteBuffer rgba) {
        int pixelBytes = Math.multiplyExact(
            Math.multiplyExact(width, height),
            4
        );

        if (rgba.remaining() != pixelBytes) {
            throw new IllegalArgumentException(
                "Unexpected RGBA buffer length"
            );
        }

        byte[] source = new byte[pixelBytes];
        rgba.duplicate().get(source);

        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(source);
        deflater.finish();

        ByteArrayOutputStream compressed =
            new ByteArrayOutputStream(Math.max(1024, pixelBytes / 2));

        byte[] buffer = new byte[16 * 1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            if (count <= 0) {
                break;
            }
            compressed.write(buffer, 0, count);
        }
        deflater.end();

        byte[] body = compressed.toByteArray();
        ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + body.length);
        out.putInt(MAGIC);
        out.putInt(width);
        out.putInt(height);
        out.putInt(pixelBytes);
        out.put(body);
        return out.array();
    }

    public static DecodedVideoFrame decode(
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

        byte[] compressed = new byte[in.remaining()];
        in.get(compressed);

        Inflater inflater = new Inflater();
        inflater.setInput(compressed);

        byte[] decoded = new byte[decodedBytes];

        try {
            int offset = 0;
            while (!inflater.finished() && offset < decoded.length) {
                int count = inflater.inflate(
                    decoded,
                    offset,
                    decoded.length - offset
                );

                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                } else {
                    offset += count;
                }
            }

            if (!inflater.finished() || offset != decoded.length) {
                return null;
            }
        } catch (DataFormatException invalid) {
            return null;
        } finally {
            inflater.end();
        }

        ByteBuffer pixels = ByteBuffer.allocateDirect(decoded.length);
        pixels.put(decoded);
        pixels.flip();

        return new DecodedVideoFrame(
            width,
            height,
            presentationTimeMicros,
            pixels
        );
    }
}
