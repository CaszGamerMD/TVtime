package com.caszgamermd.tvtime.client.render;

import com.caszgamermd.tvtime.block.SpeakerBlockEntity;
import com.caszgamermd.tvtime.client.audio.TvAudioAnchors;
import com.caszgamermd.tvtime.client.network.ClientChannelDirectory;
import com.caszgamermd.tvtime.client.network.ClientChannelSubscriptions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SpeakerBlockEntityRenderer
    implements BlockEntityRenderer<SpeakerBlockEntity, SpeakerBlockEntityRenderState> {

    public SpeakerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public SpeakerBlockEntityRenderState createRenderState() {
        return new SpeakerBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
        SpeakerBlockEntity blockEntity,
        SpeakerBlockEntityRenderState state,
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

        state.pos = blockEntity.getBlockPos();
        state.channel = blockEntity.channel();
        state.speakerChannel = blockEntity.speakerChannel();
        state.volume = blockEntity.volume();
        state.range = blockEntity.range();
    }

    @Override
    public void submit(
        SpeakerBlockEntityRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue,
        CameraRenderState cameraState
    ) {
        if (state.channel.isBlank()) {
            return;
        }

        ClientChannelSubscriptions.markSeen(state.channel);

        UUID sessionId = ClientChannelDirectory.sessionFor(state.channel);
        if (sessionId == null) {
            return;
        }

        TvAudioAnchors.markSeen(
            sessionId,
            state.pos,
            Vec3.atCenterOf(state.pos),
            state.speakerChannel,
            state.volume,
            state.range
        );
    }
}
