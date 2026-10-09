package com.caszgamermd.caszualtvtime.item;

import com.caszgamermd.caszualtvtime.CaszualTvTime;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItemIds {
    public static final ResourceKey<Item> TV_REMOTE = create("tv_remote");

    private ModItemIds() {
    }

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(Registries.ITEM, CaszualTvTime.id(name));
    }
}
