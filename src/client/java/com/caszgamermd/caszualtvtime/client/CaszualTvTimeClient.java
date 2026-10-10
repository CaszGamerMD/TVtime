package com.caszgamermd.caszualtvtime.client;

import com.caszgamermd.caszualtvtime.block.ModBlockEntities;
import com.caszgamermd.caszualtvtime.block.ModBlocks;
import com.caszgamermd.caszualtvtime.block.TvBlockEntity;
import com.caszgamermd.caszualtvtime.block.SpeakerBlockEntity;
import com.caszgamermd.caszualtvtime.client.render.TvBlockEntityRenderer;
import com.caszgamermd.caszualtvtime.client.render.SpeakerBlockEntityRenderer;
import com.caszgamermd.caszualtvtime.client.render.CameraBlockEntityRenderer;
import com.caszgamermd.caszualtvtime.client.network.CaszualTvTimeClientNetworking;
import com.caszgamermd.caszualtvtime.client.network.ClientChannelSubscriptions;
import com.caszgamermd.caszualtvtime.client.network.ClientChannelDirectory;
import com.caszgamermd.caszualtvtime.client.network.TestNetworkBroadcaster;
import com.caszgamermd.caszualtvtime.client.command.CaszualTvTimeClientCommands;
import com.caszgamermd.caszualtvtime.client.capture.CaptureBroadcastController;
import com.caszgamermd.caszualtvtime.client.audio.TvAudioPlaybackManager;
import com.caszgamermd.caszualtvtime.client.audio.NearbyAudioSourceScanner;
import com.caszgamermd.caszualtvtime.client.screen.TvConfigScreen;
import com.caszgamermd.caszualtvtime.client.screen.CameraControlScreen;
import com.caszgamermd.caszualtvtime.client.camera.CameraOperator;
import com.caszgamermd.caszualtvtime.client.screen.SpeakerConfigScreen;
import com.caszgamermd.caszualtvtime.client.screen.PortableTvPipConfigScreen;
import com.caszgamermd.caszualtvtime.client.hud.PortableTvHud;
import com.caszgamermd.caszualtvtime.item.ModItems;
import com.caszgamermd.caszualtvtime.item.PortableTvSettings;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public final class CaszualTvTimeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.TV, TvBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SPEAKER, SpeakerBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CAMERA, CameraBlockEntityRenderer::new);
        CaszualTvTimeClientNetworking.initialize();
        CaszualTvTimeClientCommands.initialize();
        PortableTvHud.initialize();

        UseItemCallback.EVENT.register(
            (player, level, hand) -> {
                if (!level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND
                    || !player.getMainHandItem().is(ModItems.TV_REMOTE)
                    || !player.getOffhandItem().is(ModBlocks.PORTABLE_TV.asItem())) {
                    return InteractionResult.PASS;
                }

                var settings = PortableTvSettings.read(
                    player.getOffhandItem()
                );

                Minecraft.getInstance().gui.setScreen(
                    new PortableTvPipConfigScreen(
                        settings.pipCorner()
                    )
                );

                return InteractionResult.SUCCESS;
            }
        );

        UseBlockCallback.EVENT.register(
            (player, level, hand, hitResult) -> {
                if (!level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND) {
                    return InteractionResult.PASS;
                }

                if (level.getBlockState(hitResult.getBlockPos()).is(ModBlocks.CAMERA_CONTROL_TABLE)) {
                    CameraControlScreen screen = new CameraControlScreen(hitResult.getBlockPos());
                    Minecraft.getInstance().gui.setScreen(screen);
                    screen.requestCatalog();
                    return InteractionResult.SUCCESS;
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
            CameraOperator.tick();
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
