package com.caszgamermd.caszualtvtime.client.camera;

import com.caszgamermd.caszualtvtime.block.CameraBlockEntity;
import com.caszgamermd.caszualtvtime.block.ModBlocks;
import com.caszgamermd.caszualtvtime.client.capture.CaptureBroadcastController;
import com.caszgamermd.caszualtvtime.client.capture.CaptureProfile;
import com.caszgamermd.caszualtvtime.client.media.VideoCodecMode;
import com.caszgamermd.caszualtvtime.client.screen.CameraOperatorScreen;
import com.caszgamermd.caszualtvtime.network.payload.ConfigureCameraPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import com.caszgamermd.caszualtvtime.block.TvBlock;

/**
 * Explicit opt-in HD source. A real Minecraft CLIENT renders the camera's
 * perspective; TVtime's existing Windows capture/H264 sender broadcasts
 * that client window. This is NOT a dedicated-server GPU renderer.
 *
 * To prevent void feeds, the hosting player must stay near the camera so the
 * vanilla client already possesses its chunks and entities. Server camera
 * raycasts are disabled while HD hosting. The player is not teleported.
 */
public final class CameraOperator {
    private static final double MAX_HOST_DISTANCE_SQR = 48.0 * 48.0;
    private static Active active;

    private CameraOperator() {}

    public static String begin(
        BlockPos console, BlockPos pos, String name, String channel,
        float pan, float tilt, float zoom
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (active != null) return "Already hosting a camera; press Esc to exit.";
        if (mc.player == null || mc.level == null) return "Join a world first.";
        if (channel == null || channel.isBlank()) return "Enter a TV channel first.";
        if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                > MAX_HOST_DISTANCE_SQR) {
            return "HD hosting requires a player within 48 blocks of the camera.";
        }
        if (!mc.level.hasChunkAt(pos) || !mc.level.getBlockState(pos).is(ModBlocks.CAMERA)) {
            return "The camera chunk must be loaded on your Minecraft client.";
        }
        if (CaptureBroadcastController.instance().running()) {
            return "Stop your current Windows broadcast before hosting this camera.";
        }
        if (!ClientPlayNetworking.canSend(ConfigureCameraPayload.TYPE)) {
            return "Server camera controls are not available.";
        }
        var backend = CaptureBroadcastController.instance();
        if (!backend.backend().supported()) {
            return "HD hosting requires TV Time's Windows capture helper.";
        }
        int minecraftWindow = -1;
        try {
            var windows = backend.windows();
            for (int i = 0; i < windows.size(); i++) {
                var w = windows.get(i);
                if (w.title().toLowerCase(java.util.Locale.ROOT).contains("minecraft")) {
                    minecraftWindow = i;
                    break;
                }
            }
        } catch (RuntimeException error) {
            return "Unable to list windows: " + error.getMessage();
        }
        if (minecraftWindow < 0) {
            return "Minecraft window not found. Window capture is required for HD hosting.";
        }

        var stand = new ArmorStand(
            mc.level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5
        );
        Entity oldCamera = mc.getCameraEntity();
        boolean oldHud = mc.options.hideGui;
        var oldProfile = backend.captureProfile();
        var oldCodec = backend.preferredCodec();
        var fovOption = mc.options.fov();
        int oldFov = fovOption.get();
        var candidate = new Active(
            console.immutable(), pos.immutable(), channel.trim(), stand,
            oldCamera, oldHud, oldProfile, oldCodec, oldFov, minecraftWindow
        );
        active = candidate;
        try {
            updateAngles(candidate, pan, tilt, zoom);
            // Set only the client-side viewer to the anchored camera. The
            // real player remains at the control table.
            mc.setCameraEntity(stand);
            mc.options.hideGui = true;
            fovOption.set(Math.max(30, Math.min(110,
                (int) (oldFov / Math.max(1.0, zoom)))));
            backend.setCaptureProfile(CaptureProfile.HIGH);
            backend.setPreferredCodec(VideoCodecMode.AUTO);
            // Release the legacy CPU camera broadcast before claiming its
            // channel with the existing streaming backend.
            ClientPlayNetworking.send(new ConfigureCameraPayload(
                console, pos, name, channel.trim(), false, pan, tilt, zoom
            ));
            mc.gui.setScreen(new CameraOperatorScreen());
        } catch (RuntimeException failure) {
            stop();
            return "Unable to start HD camera: " + failure.getMessage();
        }
        return null;
    }

    public static void tick() {
        Active a = active;
        if (a == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null
            || !(mc.gui.screen() instanceof CameraOperatorScreen)) {
            stop();
            return;
        }
        if (!mc.level.hasChunkAt(a.pos)
            || !mc.level.getBlockState(a.pos).is(ModBlocks.CAMERA)) {
            stop();
            mc.player.displayClientMessage(Component.literal(
                "Camera area unloaded. HD broadcast stopped."), false);
            return;
        }
        if (mc.level.getBlockEntity(a.pos) instanceof CameraBlockEntity camera) {
            updateAngles(a, camera.pan(), camera.tilt(), camera.zoom());
        }
        if (mc.getCameraEntity() != a.stand) mc.setCameraEntity(a.stand);
        if (a.ticks++ < 12 || a.started) return;
        try {
            CaptureBroadcastController.instance().start(a.minecraftWindow, a.channel);
            a.started = true;
        } catch (RuntimeException failure) {
            stop();
            mc.player.displayClientMessage(Component.literal(
                "HD camera broadcast failed: " + failure.getMessage()), false);
            mc.gui.setScreen(null);
        }
    }

    private static void updateAngles(Active a, float pan, float tilt, float zoom) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockState state = mc.level.getBlockState(a.pos);
        float yaw = state.getValue(TvBlock.FACING).toYRot() + pan;
        a.stand.setYRot(yaw);
        a.stand.setXRot(tilt);
        mc.options.fov().set(Math.max(30, Math.min(110,
            (int) (a.originalFov / Math.max(1.0, zoom)))));
    }

    public static void stop() {
        Active a = active;
        if (a == null) return;
        active = null;
        var mc = Minecraft.getInstance();
        var controller = CaptureBroadcastController.instance();
        if (a.started) controller.stop();
        if (mc.player != null) mc.setCameraEntity(a.originalCamera == null
            ? mc.player : a.originalCamera);
        mc.options.hideGui = a.originalHud;
        mc.options.fov().set(a.originalFov);
        controller.setCaptureProfile(a.originalProfile);
        controller.setPreferredCodec(a.originalCodec);
        // Camera intentionally stays OFF; operator can turn it on again
        // at the console after leaving HD mode. No stale channel collision.
    }

    private static final class Active {
        private final BlockPos console, pos;
        private final String channel;
        private final ArmorStand stand;
        private final Entity originalCamera;
        private final boolean originalHud;
        private final CaptureProfile originalProfile;
        private final VideoCodecMode originalCodec;
        private final int originalFov;
        private final int minecraftWindow;
        private int ticks;
        private boolean started;

        private Active(BlockPos console, BlockPos pos, String channel,
                       ArmorStand stand, Entity originalCamera,
                       boolean originalHud, CaptureProfile originalProfile,
                       VideoCodecMode originalCodec, int originalFov,
                       int minecraftWindow) {
            this.console = console; this.pos = pos; this.channel = channel;
            this.stand = stand; this.originalCamera = originalCamera;
            this.originalHud = originalHud; this.originalProfile = originalProfile;
            this.originalCodec = originalCodec; this.originalFov = originalFov;
            this.minecraftWindow = minecraftWindow;
        }
    }
}
