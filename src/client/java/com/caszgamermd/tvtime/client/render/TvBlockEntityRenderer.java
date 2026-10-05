package com.caszgamermd.tvtime.client.render;

import org.jetbrains.annotations.Nullable;

import com.caszgamermd.tvtime.block.TvBlockEntity;
import com.caszgamermd.tvtime.client.media.TestPatternVideo;
import com.caszgamermd.tvtime.client.media.VideoTexture;
import com.caszgamermd.tvtime.client.media.VideoTextureManager;
import com.caszgamermd.tvtime.client.network.ClientChannelSubscriptions;
import com.caszgamermd.tvtime.client.network.ClientChannelDirectory;
import com.caszgamermd.tvtime.client.audio.TvAudioAnchors;
import com.caszgamermd.tvtime.display.DisplayRect;
import com.caszgamermd.tvtime.display.VideoLayout;
import com.caszgamermd.tvtime.display.VideoLayoutCalculator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class TvBlockEntityRenderer implements BlockEntityRenderer<TvBlockEntity, TvBlockEntityRenderState> {
    private final Font font;

    public TvBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public TvBlockEntityRenderState createRenderState() {
        return new TvBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
        TvBlockEntity blockEntity,
        TvBlockEntityRenderState state,
        float tickProgress,
        Vec3 cameraPos,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(
            blockEntity,
            state,
            tickProgress,
            cameraPos,
            crumblingOverlay
        );

        DisplayRect rect = blockEntity.displayRect();
        state.anchor = blockEntity.getBlockPos().equals(rect.anchor());
        state.widthBlocks = rect.widthBlocks();
        state.heightBlocks = rect.heightBlocks();
        state.facing = rect.facing();
        state.displayMode = blockEntity.displayMode();
        state.channel = blockEntity.channel();
        state.tvAudioEnabled = blockEntity.tvAudioEnabled();
        state.anchorPos = rect.anchor();
    }

    @Override
    public void submit(
        TvBlockEntityRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue,
        CameraRenderState cameraState
    ) {
        if (!state.anchor) {
            return;
        }

        if (!state.channel.isBlank()) {
            ClientChannelSubscriptions.markSeen(state.channel);
        }

        UUID sessionId;
        if (state.channel.isBlank()) {
            sessionId = TestPatternVideo.sessionId("");
            TestPatternVideo.publishIfDue(sessionId);
        } else {
            sessionId = ClientChannelDirectory.sessionFor(state.channel);
        }

        if (sessionId != null && state.tvAudioEnabled && !state.channel.isBlank()) {
            TvAudioAnchors.markSeen(
                sessionId,
                displayWorldCenter(state)
            );
        }

        VideoTexture videoTexture = sessionId == null
            ? null
            : VideoTextureManager.get(sessionId);

        if (videoTexture != null) {
            videoTexture.updateFromLatest();
        }

        matrices.pushPose();
        moveToDisplayCenter(matrices, state);
        rotateToFacing(matrices, state.facing);

        if (videoTexture != null && videoTexture.ready()) {
            submitVideoSurface(state, matrices, queue, videoTexture);
        } else {
            submitFallbackText(state, matrices, queue);
        }

        matrices.popPose();
    }

    private static void submitVideoSurface(
        TvBlockEntityRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue,
        VideoTexture videoTexture
    ) {
        float canvasWidth = Math.max(0.1f, state.widthBlocks - 0.10f);
        float canvasHeight = Math.max(0.1f, state.heightBlocks - 0.10f);

        VideoLayout layout = VideoLayoutCalculator.calculate(
            videoTexture.width(),
            videoTexture.height(),
            canvasWidth,
            canvasHeight,
            state.displayMode
        );

        float canvasLeft = -canvasWidth / 2.0f;
        float canvasBottom = -canvasHeight / 2.0f;

        float left = canvasLeft + layout.x();
        float bottom = canvasBottom + layout.y();
        float right = left + layout.width();
        float top = bottom + layout.height();

        float u0 = layout.u0();
        float v0 = layout.v0();
        float u1 = layout.u1();
        float v1 = layout.v1();

        queue.submitCustomGeometry(
            matrices,
            RenderTypes.entityTranslucent(videoTexture.textureId()),
            (pose, buffer) -> {
                // UV origin is top-left; world-space Y grows upward.
                vertex(pose, buffer, left, bottom, 0.002f, u0, v1, state.lightCoords);
                vertex(pose, buffer, right, bottom, 0.002f, u1, v1, state.lightCoords);
                vertex(pose, buffer, right, top, 0.002f, u1, v0, state.lightCoords);
                vertex(pose, buffer, left, top, 0.002f, u0, v0, state.lightCoords);
            }
        );
    }

    private void submitFallbackText(
        TvBlockEntityRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue
    ) {
        String title = state.channel.isBlank() ? "TVtime TEST" : "TVtime • " + state.channel;
        String detail = state.widthBlocks + "x" + state.heightBlocks + " • " + state.displayMode.name();

        float availableWidth = Math.max(0.25f, state.widthBlocks - 0.20f);
        float titleScale = Math.min(0.16f, availableWidth / Math.max(1, font.width(title)));
        float detailScale = Math.min(0.08f, availableWidth / Math.max(1, font.width(detail)));

        matrices.pushPose();
        matrices.translate(0.0, 0.12, 0.0);
        matrices.scale(titleScale, -titleScale, titleScale);
        queue.submitText(
            matrices,
            -font.width(title) / 2.0f,
            -font.lineHeight / 2.0f,
            Component.literal(title).getVisualOrderText(),
            false,
            Font.DisplayMode.SEE_THROUGH,
            state.lightCoords,
            0xffffffff,
            0,
            0
        );
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(0.0, -0.16, 0.0);
        matrices.scale(detailScale, -detailScale, detailScale);
        queue.submitText(
            matrices,
            -font.width(detail) / 2.0f,
            -font.lineHeight / 2.0f,
            Component.literal(detail).getVisualOrderText(),
            false,
            Font.DisplayMode.SEE_THROUGH,
            state.lightCoords,
            0xff7fffd4,
            0,
            0
        );
        matrices.popPose();
    }

    private static void vertex(
        PoseStack.Pose pose,
        VertexConsumer buffer,
        float x,
        float y,
        float z,
        float u,
        float v,
        int light
    ) {
        buffer.addVertex(pose, x, y, z)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0, 0, 1);
    }

    private static Vec3 displayWorldCenter(TvBlockEntityRenderState state) {
        Direction right = rightFor(state.facing);
        double halfSpan = (state.widthBlocks - 1) / 2.0;
        double x = state.anchorPos.getX() + 0.5 + right.getStepX() * halfSpan;
        double z = state.anchorPos.getZ() + 0.5 + right.getStepZ() * halfSpan;
        double y = state.anchorPos.getY() + state.heightBlocks / 2.0;
        return new Vec3(x, y, z);
    }

    private static void moveToDisplayCenter(PoseStack matrices, TvBlockEntityRenderState state) {
        Direction right = rightFor(state.facing);
        double halfSpan = (state.widthBlocks - 1) / 2.0;

        double x = 0.5 + right.getStepX() * halfSpan + state.facing.getStepX() * 0.501;
        double z = 0.5 + right.getStepZ() * halfSpan + state.facing.getStepZ() * 0.501;
        double y = state.heightBlocks / 2.0;

        matrices.translate(x, y, z);
    }

    private static void rotateToFacing(PoseStack matrices, Direction facing) {
        float degrees = switch (facing) {
            case SOUTH -> 0.0f;
            case WEST -> -90.0f;
            case NORTH -> 180.0f;
            case EAST -> 90.0f;
            default -> 0.0f;
        };
        matrices.mulPose(Axis.YP.rotationDegrees(degrees));
    }

    private static Direction rightFor(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }
}
