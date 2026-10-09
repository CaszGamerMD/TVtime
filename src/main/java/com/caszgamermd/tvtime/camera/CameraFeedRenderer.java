package com.caszgamermd.tvtime.camera;

import com.caszgamermd.tvtime.block.CameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.nio.ByteBuffer;

/**
 * An inexpensive server-side perspective camera. Produces actual LIVE block scene changes,
 * but not yet Minecraft's native textured/entity renderer (which runs on the client GPU).
 * Bounded range/resolution avoid synchronous chunk loads and heavy frame costs.
 */
public final class CameraFeedRenderer {
    public static final int WIDTH = 48;
    public static final int HEIGHT = 27;
    private static final int MAGIC = 0x54565230; // TVR0, supported by TVtime clients
    private static final double RANGE = 40.0;
    private static final double STEP = 0.60;

    private CameraFeedRenderer() {}

    public static byte[] render(ServerLevel level, CameraBlockEntity camera) {
        ByteBuffer frame = ByteBuffer.allocate(12 + WIDTH * HEIGHT * 4);
        frame.putInt(MAGIC).putInt(WIDTH).putInt(HEIGHT);
        BlockPos origin = camera.getBlockPos();
        double ox = origin.getX() + 0.5;
        double oy = origin.getY() + 0.61;
        double oz = origin.getZ() + 0.5;
        double yaw = Math.toRadians(camera.yawDegrees());
        double pitch = Math.toRadians(camera.tilt());
        double sinYaw = Math.sin(yaw), cosYaw = Math.cos(yaw);
        double sinPitch = Math.sin(pitch), cosPitch = Math.cos(pitch);
        double fx = -sinYaw * cosPitch, fy = -sinPitch, fz = cosYaw * cosPitch;
        double rx = cosYaw, rz = sinYaw;
        double ux = sinYaw * sinPitch, uy = cosPitch, uz = -cosYaw * sinPitch;
        double tangent = Math.tan(Math.toRadians(34.0 / camera.zoom()));
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos();
        int time = (int) (level.getDayTime() % 24000L);

        for (int y = 0; y < HEIGHT; y++) {
            double vertical = (1.0 - (y + 0.5) * 2.0 / HEIGHT) * tangent;
            for (int x = 0; x < WIDTH; x++) {
                double horizontal = ((x + 0.5) * 2.0 / WIDTH - 1.0) * tangent * WIDTH / HEIGHT;
                double dx = fx + rx * horizontal + ux * vertical;
                double dy = fy + uy * vertical;
                double dz = fz + rz * horizontal + uz * vertical;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= length; dy /= length; dz /= length;
                int rgb = 0;
                double at = 0;
                boolean hit = false;
                for (double d = 0.5; d < RANGE; d += STEP) {
                    double px = ox + dx * d, py = oy + dy * d, pz = oz + dz * d;
                    sample.set((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
                    if (py < level.getMinY() || py >= level.getMaxY() || !level.hasChunkAt(sample)) {
                        at = d;
                        break;
                    }
                    BlockState state = level.getBlockState(sample);
                    if (state.isAir()) continue;
                    // Color from the actual block state; ray depth adds distance shading.
                    rgb = state.getMapColor(level, sample).col;
                    if (rgb == 0) rgb = 0x777777;
                    at = d;
                    hit = true;
                    break;
                }
                if (!hit) {
                    // Sky, void or untransmitted distant terrain.
                    int blue = time > 12500 && time < 23500 ? 46 : 143;
                    rgb = dy > 0 ? (0x263450 + ((blue / 2) << 8)) : (0x333535);
                } else {
                    double shade = Math.max(0.22, 1.0 - at / (RANGE * 1.3));
                    int r = (int)(((rgb >> 16) & 255) * shade);
                    int g = (int)(((rgb >> 8) & 255) * shade);
                    int b = (int)((rgb & 255) * shade);
                    rgb = (r << 16) | (g << 8) | b;
                }
                frame.put((byte) (rgb >> 16));
                frame.put((byte) (rgb >> 8));
                frame.put((byte) rgb);
                frame.put((byte) 255);
            }
        }
        return frame.array();
    }
}
