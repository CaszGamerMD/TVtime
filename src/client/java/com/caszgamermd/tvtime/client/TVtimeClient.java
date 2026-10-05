package com.caszgamermd.tvtime.client;

import com.caszgamermd.tvtime.block.ModBlockEntities;
import com.caszgamermd.tvtime.client.render.TvBlockEntityRenderer;
import com.caszgamermd.tvtime.client.network.TVtimeClientNetworking;
import com.caszgamermd.tvtime.client.network.ClientChannelSubscriptions;
import com.caszgamermd.tvtime.client.network.ClientChannelDirectory;
import com.caszgamermd.tvtime.client.network.TestNetworkBroadcaster;
import com.caszgamermd.tvtime.client.command.TVtimeClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class TVtimeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.TV, TvBlockEntityRenderer::new);
        TVtimeClientNetworking.initialize();
        TVtimeClientCommands.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                ClientChannelSubscriptions.clear();
                ClientChannelDirectory.clear();
                TestNetworkBroadcaster.stop();
            } else {
                ClientChannelSubscriptions.tick();
                TestNetworkBroadcaster.tick();
            }
        });
    }
}
