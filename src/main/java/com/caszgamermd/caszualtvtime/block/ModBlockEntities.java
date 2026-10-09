package com.caszgamermd.caszualtvtime.block;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<TvBlockEntity> TV = register(
        "tv",
        TvBlockEntity::new,
        ModBlocks.TV,
        ModBlocks.PORTABLE_TV
    );

    public static final BlockEntityType<CameraBlockEntity> CAMERA = register(
        "camera",
        CameraBlockEntity::new,
        ModBlocks.CAMERA
    );

    public static final BlockEntityType<SpeakerBlockEntity> SPEAKER = register(
        "speaker",
        SpeakerBlockEntity::new,
        ModBlocks.SPEAKER,
        ModBlocks.IRON_SPEAKER,
        ModBlocks.SPRUCE_SPEAKER,
        ModBlocks.MODERN_SPEAKER,
        ModBlocks.CUSTOM_SPEAKER
    );

    private ModBlockEntities() {
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(
        String name,
        FabricBlockEntityTypeBuilder.Factory<? extends T> factory,
        Block... blocks
    ) {
        return Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CaszualTvTime.id(name),
            FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build()
        );
    }

    public static void initialize() {
    }
}
