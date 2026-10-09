package com.caszgamermd.caszualcaszual_tv_time.client;

import com.caszgamermd.caszualcaszual_tv_time.block.ModBlockEntities;
import com.caszgamermd.caszualcaszual_tv_time.block.ModBlocks;
import com.caszgamermd.caszualcaszual_tv_time.block.TvBlockEntity;
import com.caszgamermd.caszualcaszual_tv_time.block.SpeakerBlockEntity;
import com.caszgamermd.caszualcaszual_tv_time.client.render.TvBlockEntityRenderer;
import com.caszgamermd.caszualcaszual_tv_time.client.render.SpeakerBlockEntityRenderer;
import com.caszgamermd.caszualcaszual_tv_time.client.network.CaszualTvTimeClientNetworking;
import com.caszgamermd.caszualcaszual_tv_time.client.network.ClientChannelSubscriptions;
import com.caszgamermd.caszualcaszual_tv_time.client.network.ClientChannelDirectory;
import com.caszgamermd.caszualcaszual_tv_time.client.network.TestNetworkBroadcaster;
import com.caszgamermd.caszualcaszual_tv_time.client.command.CaszualTvTimeClientCommands;
import com.caszgamermd.caszualcaszual_tv_time.client.capture.CaptureBroadcastController;
import com.caszgamermd.caszualcaszual_tv_time.client.audio.TvAudioPlaybackManager;
import com.caszgamermd.caszualcaszual_tv_time.client.audio.NearbyAudioSourceScanner;
import com.caszgamermd.caszualcaszual_tv_time.client.screen.TvConfigScreen;
import com.caszgamermd.caszualcaszual_tv_time.client.screen.CameraControlScreen;
import com.caszgamermd.caszualcaszual_tv_time.client.screen.SpeakerConfigScreen;
import com.caszgamermd.caszualcaszual_tv_time.client.screen.PortableTvPipConfigScreen;
import com.caszgamermd.caszualcaszual_tv_time.client.hud.PortableTvHud;
import com.caszgamermd.caszualcaszual_tv_time.item.ModItems;
import com.caszgamermd.caszualcaszual_tv_time.item.PortableTvSettings;
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
