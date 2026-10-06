package com.caszgamermd.tvtime.block;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final Block TV = register(
        ModBlockItemIds.TV,
        TvBlock::new,
        BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion()
    );

    public static final Block SPEAKER = register(
        ModBlockItemIds.SPEAKER,
        SpeakerBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.5f, 3.0f)
            .sound(SoundType.WOOD)
    );

    private ModBlocks() {
    }

    private static Block register(
        ResourceKey<Block> id,
        Function<BlockBehaviour.Properties, Block> blockFactory,
        BlockBehaviour.Properties properties
    ) {
        Block block = blockFactory.apply(properties.setId(id));
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block register(
        BlockItemId id,
        Function<BlockBehaviour.Properties, Block> blockFactory,
        BlockBehaviour.Properties properties
    ) {
        Block block = register(id.block(), blockFactory, properties);
        BlockItem blockItem = new BlockItem(
            block,
            new Item.Properties().useBlockDescriptionPrefix().setId(id.item())
        );
        Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);
        return block;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(TV.asItem());
            entries.accept(SPEAKER.asItem());
        });
    }
}
