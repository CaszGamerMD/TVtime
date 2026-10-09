package com.caszgamermd.caszualcaszual_tv_time;

import com.caszgamermd.caszualcaszual_tv_time.block.ModBlockEntities;
import com.caszgamermd.caszualcaszual_tv_time.camera.CameraManager;
import com.caszgamermd.caszualcaszual_tv_time.block.ModBlocks;
import com.caszgamermd.caszualcaszual_tv_time.broadcast.BroadcastManager;
import com.caszgamermd.caszualcaszual_tv_time.network.CaszualTvTimeNetworking;
import com.caszgamermd.caszualcaszual_tv_time.item.ModItems;
import com.caszgamermd.caszualcaszual_tv_time.item.PortableTvSettings;
import com.caszgamermd.caszualcaszual_tv_time.block.TvBlockEntity;
import com.caszgamermd.caszualcaszual_tv_time.block.SpeakerBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CaszualTvTime implements ModInitializer {
    public static final String MOD_ID = "caszual_tv_time";
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
        CaszualTvTimeNetworking.initialize();
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

        LOGGER.info("Initializing Caszual TV Time");
    }
}
