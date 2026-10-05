package com.caszgamermd.tvtime.block;

import com.caszgamermd.tvtime.TVtime;
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
        ModBlocks.TV
    );

    public static final BlockEntityType<SpeakerBlockEntity> SPEAKER = register(
        "speaker",
        SpeakerBlockEntity::new,
        ModBlocks.SPEAKER
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
            TVtime.id(name),
            FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build()
        );
    }

    public static void initialize() {
    }
}
