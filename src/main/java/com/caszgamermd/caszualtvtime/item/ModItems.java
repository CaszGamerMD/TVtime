package com.caszgamermd.caszualcaszual_tv_time.item;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Item TV_REMOTE = register(
        ModItemIds.TV_REMOTE,
        Item::new,
        new Item.Properties().stacksTo(1)
    );

    private ModItems() {
    }

    private static Item register(
        ResourceKey<Item> key,
        Function<Item.Properties, Item> factory,
        Item.Properties properties
    ) {
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        CreativeModeTabEvents
            .modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(entries -> entries.accept(TV_REMOTE));
    }
}
