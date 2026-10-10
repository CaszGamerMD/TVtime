package com.caszgamermd.caszualtvtime.camera;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/**
 * TVZ1 is already understood by TV Time's video decoder. Compressing once per
 * camera is far cheaper than sending uncompressed RGBA to every subscriber.
 *
 * This runs after the bounded main-thread scene read, not for each viewer.
 */
public final class CameraFrameEncoder {
    private static final int MAGIC = 0x54565a31; // TVZ1
    private CameraFrameEncoder() {}

    public static byte[] encode(int width, int height, byte[] rawFrame) {
        int pixels = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rawFrame.length != pixels + 12) {
            throw new IllegalArgumentException("Unexpected camera pixel buffer size");
        }
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(rawFrame, 12, pixels);
            deflater.finish();
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(1024, pixels / 4));
            byte[] scratch = new byte[8192];
            while (!deflater.finished()) {
                int n = deflater.deflate(scratch);
                if (n <= 0) throw new IllegalStateException("Stalled camera compression");
                output.write(scratch, 0, n);
            }
            byte[] body = output.toByteArray();
            ByteBuffer packet = ByteBuffer.allocate(16 + body.length);
            packet.putInt(MAGIC).putInt(width).putInt(height).putInt(pixels).put(body);
            return packet.array();
        } finally {
            deflater.end();
        }
    }
}
