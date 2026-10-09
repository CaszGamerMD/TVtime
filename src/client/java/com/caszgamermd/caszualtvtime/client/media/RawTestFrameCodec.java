package com.caszgamermd.caszualcaszual_tv_time.client.media;

import java.nio.ByteBuffer;

public final class RawTestFrameCodec {
    private static final int MAGIC = 0x54565230; // TVR0
    private static final int HEADER_BYTES = 12;

    private RawTestFrameCodec() {
    }

    public static byte[] encode(int width, int height, ByteBuffer rgba) {
        int pixelBytes = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgba.remaining() != pixelBytes) {
            throw new IllegalArgumentException("Unexpected RGBA buffer length");
        }

        ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + pixelBytes);
        out.putInt(MAGIC);
        out.putInt(width);
        out.putInt(height);

        ByteBuffer source = rgba.duplicate();
        out.put(source);
        return out.array();
    }

    public static DecodedVideoFrame decode(byte[] packet, long presentationTimeMicros) {
        if (packet == null || packet.length < HEADER_BYTES) {
            return null;
        }

        ByteBuffer in = ByteBuffer.wrap(packet);
        if (in.getInt() != MAGIC) {
            return null;
        }

        int width = in.getInt();
        int height = in.getInt();
        if (width <= 0 || height <= 0) {
            return null;
        }

        long expected = (long) width * height * 4L;
        if (expected > Integer.MAX_VALUE || in.remaining() != (int) expected) {
            return null;
        }

        ByteBuffer pixels = ByteBuffer.allocateDirect((int) expected);
        pixels.put(in);
        pixels.flip();

        return new DecodedVideoFrame(width, height, presentationTimeMicros, pixels);
    }
}
