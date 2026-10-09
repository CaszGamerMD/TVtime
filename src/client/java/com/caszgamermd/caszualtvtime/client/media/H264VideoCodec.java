package com.caszgamermd.caszualcaszual_tv_time.client.media;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

import java.nio.ByteBuffer;

/**
 * Thin CaszualTvTime framing around JCodec's baseline H.264 encoder/decoder.
 *
 * Each packet contains one Annex-B access unit. IDR packets include SPS/PPS
 * because JCodec emits them on keyframes.
 */
public final class H264VideoCodec {
    private static final int MAGIC = 0x54564831; // TVH1
    private static final int HEADER_BYTES = 13;
    private static final int FLAG_KEYFRAME = 1;
    private static final int MAX_DIMENSION = 4096;

    private H264VideoCodec() {
    }

    public static final class Encoder {
        private final H264Encoder encoder;
        private int width = -1;
        private int height = -1;
        private Picture rgb;
        private Picture yuv;
        private Transform rgbToYuv;

        public Encoder(int keyInterval) {
            encoder = H264Encoder.createH264Encoder();
            encoder.setKeyInterval(Math.max(1, keyInterval));
        }

        public Encoded encode(
            int width,
            int height,
            ByteBuffer rgba
        ) {
            validateDimensions(width, height);

            int expected = Math.multiplyExact(
                Math.multiplyExact(width, height),
                4
            );

            if (rgba.remaining() != expected) {
                throw new IllegalArgumentException(
                    "Unexpected RGBA buffer length"
                );
            }

            ensurePictures(width, height);
            fillRgb(rgb, rgba);

            rgbToYuv.transform(rgb, yuv);

            int estimated = Math.max(
                64 * 1024,
                encoder.estimateBufferSize(yuv) * 2
            );

            ByteBuffer output = ByteBuffer.allocate(estimated);
            VideoEncoder.EncodedFrame encoded =
                encoder.encodeFrame(yuv, output);

            ByteBuffer data = encoded.getData().duplicate();
            byte[] body = new byte[data.remaining()];
            data.get(body);

            ByteBuffer framed = ByteBuffer.allocate(
                HEADER_BYTES + body.length
            );
            framed.putInt(MAGIC);
            framed.putInt(width);
            framed.putInt(height);
            framed.put((byte) (
                encoded.isKeyFrame() ? FLAG_KEYFRAME : 0
            ));
            framed.put(body);

            return new Encoded(
                framed.array(),
                encoded.isKeyFrame()
            );
        }

        public void reset() {
            width = -1;
            height = -1;
            rgb = null;
            yuv = null;
            rgbToYuv = null;
        }

        private void ensurePictures(int width, int height) {
            if (this.width == width
                && this.height == height
                && rgb != null
                && yuv != null) {
                return;
            }

            this.width = width;
            this.height = height;

            rgb = Picture.create(
                width,
                height,
                ColorSpace.RGB
            );
            yuv = Picture.create(
                width,
                height,
                ColorSpace.YUV420J
            );

            rgbToYuv = ColorUtil.getTransform(
                ColorSpace.RGB,
                ColorSpace.YUV420J
            );

            if (rgbToYuv == null) {
                throw new IllegalStateException(
                    "JCodec RGB->YUV420J transform unavailable"
                );
            }
        }
    }

    public static final class Decoder {
        private H264Decoder decoder = new H264Decoder();

        public DecodedVideoFrame decode(
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
            in.get(); // flags are transport metadata; decoder reads NAL types.

            try {
                validateDimensions(width, height);
            } catch (IllegalArgumentException invalid) {
                return null;
            }

            byte[] accessUnit = new byte[in.remaining()];
            in.get(accessUnit);

            int codedWidth = (width + 15) & ~15;
            int codedHeight = (height + 15) & ~15;

            Picture bufferPicture = Picture.create(
                codedWidth,
                codedHeight,
                ColorSpace.YUV420
            );

            final Picture decoded;
            try {
                decoded = decoder.decodeFrame(
                    ByteBuffer.wrap(accessUnit),
                    bufferPicture.getData()
                );
            } catch (RuntimeException badFrame) {
                return null;
            }

            if (decoded == null) {
                return null;
            }

            Picture visible = decoded.cropped();

            Transform toRgb = ColorUtil.getTransform(
                visible.getColor(),
                ColorSpace.RGB
            );

            if (toRgb == null) {
                return null;
            }

            Picture rgb = Picture.create(
                visible.getWidth(),
                visible.getHeight(),
                ColorSpace.RGB
            );

            toRgb.transform(visible, rgb);

            int outWidth = Math.min(width, rgb.getWidth());
            int outHeight = Math.min(height, rgb.getHeight());

            ByteBuffer rgba = ByteBuffer.allocateDirect(
                Math.multiplyExact(
                    Math.multiplyExact(outWidth, outHeight),
                    4
                )
            );

            byte[] data = rgb.getPlaneData(0);
            int stride = rgb.getWidth() * 3;

            for (int y = 0; y < outHeight; y++) {
                int row = y * stride;

                for (int x = 0; x < outWidth; x++) {
                    int offset = row + x * 3;

                    rgba.put((byte) ((data[offset] + 128) & 0xff));
                    rgba.put((byte) ((data[offset + 1] + 128) & 0xff));
                    rgba.put((byte) ((data[offset + 2] + 128) & 0xff));
                    rgba.put((byte) 255);
                }
            }

            rgba.flip();

            return new DecodedVideoFrame(
                outWidth,
                outHeight,
                presentationTimeMicros,
                rgba
            );
        }

        public void reset() {
            decoder = new H264Decoder();
        }
    }

    public static boolean isH264Packet(byte[] packet) {
        return packet != null
            && packet.length >= 4
            && ByteBuffer.wrap(packet).getInt() == MAGIC;
    }

    public static boolean isKeyFrame(byte[] packet) {
        return packet != null
            && packet.length >= HEADER_BYTES
            && ByteBuffer.wrap(packet).getInt() == MAGIC
            && (packet[12] & FLAG_KEYFRAME) != 0;
    }

    private static void fillRgb(
        Picture rgb,
        ByteBuffer rgba
    ) {
        ByteBuffer source = rgba.duplicate();
        byte[] target = rgb.getPlaneData(0);

        int pixel = 0;
        while (source.remaining() >= 4) {
            target[pixel++] =
                (byte) ((source.get() & 0xff) - 128);
            target[pixel++] =
                (byte) ((source.get() & 0xff) - 128);
            target[pixel++] =
                (byte) ((source.get() & 0xff) - 128);
            source.get(); // alpha
        }
    }

    private static void validateDimensions(
        int width,
        int height
    ) {
        if (width <= 0
            || height <= 0
            || width > MAX_DIMENSION
            || height > MAX_DIMENSION
            || (width & 1) != 0
            || (height & 1) != 0) {
            throw new IllegalArgumentException(
                "H264 dimensions must be positive, even, and <= "
                    + MAX_DIMENSION
            );
        }
    }

    public record Encoded(
        byte[] payload,
        boolean keyFrame
    ) {
    }
}
