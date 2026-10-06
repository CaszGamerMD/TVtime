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
import net.minecraft.world.level.block.state.BlockState;

public final class ModBlocks {
    public static final Block TV = register(
        ModBlockItemIds.TV,
        TvBlock::new,
        BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion()
    );

    // Legacy block kept registered so existing alpha worlds do not lose
    // already-placed speakers. It is intentionally hidden from the tab.
    public static final Block SPEAKER = register(
        ModBlockItemIds.SPEAKER,
        SpeakerBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.5f, 3.0f)
            .sound(SoundType.WOOD)
    );

    public static final Block IRON_SPEAKER = register(
        ModBlockItemIds.IRON_SPEAKER,
        SpeakerBlock::new,
        BlockBehaviour.Properties.of()
            .strength(3.0f, 8.0f)
            .sound(SoundType.METAL)
            .noOcclusion()
    );

    public static final Block SPRUCE_SPEAKER = register(
        ModBlockItemIds.SPRUCE_SPEAKER,
        SpeakerBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.8f, 4.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()
    );

    public static final Block MODERN_SPEAKER = register(
        ModBlockItemIds.MODERN_SPEAKER,
        SpeakerBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.5f, 4.0f)
            .sound(SoundType.METAL)
            .noOcclusion()
    );

    public static final Block CUSTOM_SPEAKER = register(
        ModBlockItemIds.CUSTOM_SPEAKER,
        SpeakerBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.8f, 4.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()
    );

    private ModBlocks() {
    }

    public static boolean isSpeaker(BlockState state) {
        return state.is(SPEAKER)
            || state.is(IRON_SPEAKER)
            || state.is(SPRUCE_SPEAKER)
            || state.is(MODERN_SPEAKER)
            || state.is(CUSTOM_SPEAKER);
    }

    public static SpeakerStyle speakerStyle(Block block) {
        if (block == IRON_SPEAKER) {
            return SpeakerStyle.IRON;
        }
        if (block == SPRUCE_SPEAKER) {
            return SpeakerStyle.SPRUCE;
        }
        if (block == MODERN_SPEAKER) {
            return SpeakerStyle.MODERN;
        }
        if (block == CUSTOM_SPEAKER) {
            return SpeakerStyle.CUSTOM;
        }
        return SpeakerStyle.LEGACY;
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
            entries.accept(IRON_SPEAKER.asItem());
            entries.accept(SPRUCE_SPEAKER.asItem());
            entries.accept(MODERN_SPEAKER.asItem());
            entries.accept(CUSTOM_SPEAKER.asItem());
        });
    }
}
