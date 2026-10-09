package com.caszgamermd.tvtime;

import com.caszgamermd.tvtime.block.ModBlockEntities;
import com.caszgamermd.tvtime.camera.CameraManager;
import com.caszgamermd.tvtime.block.ModBlocks;
import com.caszgamermd.tvtime.broadcast.BroadcastManager;
import com.caszgamermd.tvtime.network.TVtimeNetworking;
import com.caszgamermd.tvtime.item.ModItems;
import com.caszgamermd.tvtime.item.PortableTvSettings;
import com.caszgamermd.tvtime.block.TvBlockEntity;
import com.caszgamermd.tvtime.block.SpeakerBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TVtime implements ModInitializer {
    public static final String MOD_ID = "tvtime";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final BroadcastManager BROADCAST_MANAGER = new BroadcastManager();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static BroadcastManager broadcasts() {
        return BROADCAST_MANAGER;
    }

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModItems.initialize();
        ModBlockEntities.initialize();
        TVtimeNetworking.initialize();
        CameraManager.initialize();

        UseBlockCallback.EVENT.register(
            (player, level, hand, hitResult) -> {
                if (hand != InteractionHand.MAIN_HAND) {
                    return InteractionResult.PASS;
                }

                var held = player.getItemInHand(hand);
                var pos = hitResult.getBlockPos();
                var state = level.getBlockState(pos);

                if (held.is(ModBlocks.PORTABLE_TV.asItem())
                    && state.is(ModBlocks.TV)
                    && level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
                    if (!level.isClientSide()) {
                        PortableTvSettings.copyFrom(held, tv);
                    }
                    return InteractionResult.SUCCESS;
                }

                if (!player.isShiftKeyDown()
                    || held.is(ModItems.TV_REMOTE)
                    || !state.is(ModBlocks.CUSTOM_SPEAKER)) {
                    return InteractionResult.PASS;
                }

                if (!level.isClientSide()) {
                    var next = state
                        .getValue(SpeakerBlock.SIDE_STYLE)
                        .next();

                    level.setBlock(
                        pos,
                        state.setValue(
                            SpeakerBlock.SIDE_STYLE,
                            next
                        ),
                        Block.UPDATE_ALL
                    );
                }

                return InteractionResult.SUCCESS;
            }
        );

        LOGGER.info("Initializing TVtime");
    }
}
