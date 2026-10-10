package com.caszgamermd.caszualtvtime.camera;

import com.caszgamermd.caszualtvtime.block.CameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.ByteBuffer;

/**
 * Small server-side block-color scene renderer (not the client GPU renderer).
 *
 * Camera rendering is incremental. Main-thread block access is spread over many ticks,
 * and the manager enforces a global ray budget shared by every live camera. Voxel DDA
 * visits a block only once per ray instead of sampling the same block at 0.6-block steps.
 */
public final class CameraFeedRenderer {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 72;
    public static final int FPS_LIMIT = 2;
    public static final int FRAME_INTERVAL_TICKS = 20 / FPS_LIMIT;
    public static final int TOTAL_RAYS_PER_TICK = 512;
    private static final int MAGIC = 0x54565230; // TVR0; uses existing TVtime RGBA decoder
    private static final double RANGE = 40.0;

    private CameraFeedRenderer() {}

    public static final class Frame {
        private final BlockPos origin;
        private final float yaw, pitch, zoom;
        private final double ox, oy, oz;
        private final double fx, fy, fz;
        private final double rx, rz, ux, uy, uz;
        private final double tangent;
        private final byte[] rgba = new byte[12 + WIDTH * HEIGHT * 4];
        private int nextPixel;

        public Frame(CameraBlockEntity camera) {
            origin = camera.getBlockPos().immutable();
            yaw = camera.yawDegrees();
            pitch = camera.tilt();
            zoom = camera.zoom();
            ox = origin.getX() + 0.5;
            oy = origin.getY() + 0.61;
            oz = origin.getZ() + 0.5;
            double a = Math.toRadians(yaw), b = Math.toRadians(pitch);
            double sy = Math.sin(a), cy = Math.cos(a);
            double sp = Math.sin(b), cp = Math.cos(b);
            fx = -sy * cp; fy = -sp; fz = cy * cp;
            rx = cy; rz = sy;
            ux = sy * sp; uy = cp; uz = -cy * sp;
            tangent = Math.tan(Math.toRadians(34.0 / zoom));
            ByteBuffer.wrap(rgba).putInt(MAGIC).putInt(WIDTH).putInt(HEIGHT);
        }

        public boolean matches(CameraBlockEntity camera) {
            return origin.equals(camera.getBlockPos())
                && Float.compare(yaw, camera.yawDegrees()) == 0
                && Float.compare(pitch, camera.tilt()) == 0
                && Float.compare(zoom, camera.zoom()) == 0;
        }

        /** Run at most 'budget' rays on the server thread; true when the frame is complete. */
        public boolean renderNext(ServerLevel level, int budget) {
            BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
            int end = Math.min(WIDTH * HEIGHT, nextPixel + Math.max(0, budget));
            for (; nextPixel < end; nextPixel++) {
                int x = nextPixel % WIDTH;
                int y = nextPixel / WIDTH;
                double horizontal = (((x + 0.5) * 2.0 / WIDTH) - 1.0) * tangent * WIDTH / HEIGHT;
                double vertical = (1.0 - (y + 0.5) * 2.0 / HEIGHT) * tangent;
                double dx = fx + rx * horizontal + ux * vertical;
                double dy = fy + uy * vertical;
                double dz = fz + rz * horizontal + uz * vertical;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                int rgb = cast(level, sample, dx / length, dy / length, dz / length);
                int offset = 12 + nextPixel * 4;
                rgba[offset] = (byte) (rgb >>> 16);
                rgba[offset + 1] = (byte) (rgb >>> 8);
                rgba[offset + 2] = (byte) rgb;
                rgba[offset + 3] = (byte) 255;
            }
            return nextPixel == WIDTH * HEIGHT;
        }

        /**
         * Amanatides–Woo voxel traversal. Uses strictly loaded terrain, never forces
         * terrain generation. The camera's own block is skipped so the lens can see out.
         */
        private int cast(ServerLevel level, BlockPos.MutableBlockPos sample,
                         double dx, double dy, double dz) {
            int x = (int) Math.floor(ox);
            int y = (int) Math.floor(oy);
            int z = (int) Math.floor(oz);
            int sx = dx > 0 ? 1 : dx < 0 ? -1 : 0;
            int sy = dy > 0 ? 1 : dy < 0 ? -1 : 0;
            int sz = dz > 0 ? 1 : dz < 0 ? -1 : 0;
            double deltaX = sx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
            double deltaY = sy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
            double deltaZ = sz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
            double tx = sx == 0 ? Double.POSITIVE_INFINITY
                : ((sx > 0 ? x + 1.0 - ox : ox - x) * deltaX);
            double ty = sy == 0 ? Double.POSITIVE_INFINITY
                : ((sy > 0 ? y + 1.0 - oy : oy - y) * deltaY);
            double tz = sz == 0 ? Double.POSITIVE_INFINITY
                : ((sz > 0 ? z + 1.0 - oz : oz - z) * deltaZ);
            double depth = 0.0;
            int face = 1;

            while (depth < RANGE && y >= level.getMinY() && y < level.getMaxY()) {
                sample.set(x, y, z);
                if (!sample.equals(origin)) {
                    // hasChunkAt is a non-loading check, unlike getChunkAt.
                    if (!level.hasChunkAt(sample)) break;
                    BlockState block = level.getBlockState(sample);
                    if (!block.isAir()) {
                        int rgb = block.getMapColor(level, sample).col;
                        if (rgb == 0) rgb = 0x777777;
                        double light = Math.max(0.22, 1.0 - depth / (RANGE * 1.3));
                        // Orient the fake geometry with simple face lighting.
                        light *= face == 0 ? 0.83 : face == 2 ? 0.72 : 1.0;
                        int red = (int) (((rgb >>> 16) & 255) * light);
                        int green = (int) (((rgb >>> 8) & 255) * light);
                        int blue = (int) ((rgb & 255) * light);
                        return red << 16 | green << 8 | blue;
                    }
                }
                if (tx < ty && tx < tz) {
                    depth = tx; tx += deltaX; x += sx; face = 0;
                } else if (ty < tz) {
                    depth = ty; ty += deltaY; y += sy; face = 1;
                } else {
                    depth = tz; tz += deltaZ; z += sz; face = 2;
                }
            }

            // Consistent sky color without sampling game time on every pixel.
            return dy > 0.0 ? 0x6186af : 0x313b40;
        }

        public byte[] data() {
            if (nextPixel != WIDTH * HEIGHT) throw new IllegalStateException("Incomplete camera frame");
            return rgba;
        }
    }
}
