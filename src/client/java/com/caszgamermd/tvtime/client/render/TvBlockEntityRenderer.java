package com.caszgamermd.tvtime.client.render;

import org.jetbrains.annotations.Nullable;

import com.caszgamermd.tvtime.block.TvBlockEntity;
import com.caszgamermd.tvtime.display.DisplayRect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

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

        String title = state.channel.isBlank() ? "TVtime TEST" : "TVtime • " + state.channel;
        String detail = state.widthBlocks + "x" + state.heightBlocks + " • " + state.displayMode.name();

        matrices.pushPose();
        moveToDisplayCenter(matrices, state);
        rotateToFacing(matrices, state.facing);

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
            LightTexture.FULL_BRIGHT,
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
            LightTexture.FULL_BRIGHT,
            0xff7fffd4,
            0,
            0
        );
        matrices.popPose();

        matrices.popPose();
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
