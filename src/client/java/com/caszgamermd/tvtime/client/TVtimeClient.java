package com.caszgamermd.tvtime.client;

import com.caszgamermd.tvtime.block.ModBlockEntities;
import com.caszgamermd.tvtime.client.render.TvBlockEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class TVtimeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.TV, TvBlockEntityRenderer::new);
    }
}
