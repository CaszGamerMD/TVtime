package com.caszgamermd.tvtime.client;

import com.caszgamermd.tvtime.block.ModBlockEntities;
import com.caszgamermd.tvtime.block.TvBlockEntity;
import com.caszgamermd.tvtime.block.SpeakerBlockEntity;
import com.caszgamermd.tvtime.client.render.TvBlockEntityRenderer;
import com.caszgamermd.tvtime.client.render.SpeakerBlockEntityRenderer;
import com.caszgamermd.tvtime.client.network.TVtimeClientNetworking;
import com.caszgamermd.tvtime.client.network.ClientChannelSubscriptions;
import com.caszgamermd.tvtime.client.network.ClientChannelDirectory;
import com.caszgamermd.tvtime.client.network.TestNetworkBroadcaster;
import com.caszgamermd.tvtime.client.command.TVtimeClientCommands;
import com.caszgamermd.tvtime.client.capture.CaptureBroadcastController;
import com.caszgamermd.tvtime.client.audio.TvAudioPlaybackManager;
import com.caszgamermd.tvtime.client.audio.NearbyAudioSourceScanner;
import com.caszgamermd.tvtime.client.screen.TvConfigScreen;
import com.caszgamermd.tvtime.client.screen.SpeakerConfigScreen;
import com.caszgamermd.tvtime.client.hud.PortableTvHud;
import com.caszgamermd.tvtime.item.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public final class TVtimeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.TV, TvBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SPEAKER, SpeakerBlockEntityRenderer::new);
        TVtimeClientNetworking.initialize();
        TVtimeClientCommands.initialize();
        PortableTvHud.initialize();

        UseBlockCallback.EVENT.register(
            (player, level, hand, hitResult) -> {
                if (!level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND) {
                    return InteractionResult.PASS;
                }

                if (!player.getItemInHand(hand).is(ModItems.TV_REMOTE)) {
                    return InteractionResult.PASS;
                }

                var blockEntity =
                    level.getBlockEntity(hitResult.getBlockPos());

                if (blockEntity instanceof TvBlockEntity tv) {
                    Minecraft.getInstance().gui.setScreen(
                        new TvConfigScreen(tv)
                    );
                    return InteractionResult.SUCCESS;
                }

                if (blockEntity instanceof SpeakerBlockEntity speaker) {
                    Minecraft.getInstance().gui.setScreen(
                        new SpeakerConfigScreen(speaker)
                    );
                    return InteractionResult.SUCCESS;
                }

                return InteractionResult.PASS;
            }
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                ClientChannelSubscriptions.clear();
                ClientChannelDirectory.clear();
                TestNetworkBroadcaster.stop();
                CaptureBroadcastController.instance().stop();
                TvAudioPlaybackManager.clear();
            } else {
                ClientChannelSubscriptions.tick();
                TestNetworkBroadcaster.tick();
                NearbyAudioSourceScanner.tick(client);
                TvAudioPlaybackManager.tick();
            }
        });
    }
}
