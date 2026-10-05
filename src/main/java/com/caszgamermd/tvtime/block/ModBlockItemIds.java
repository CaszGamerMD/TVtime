package com.caszgamermd.tvtime.block;

import com.caszgamermd.tvtime.TVtime;
import net.minecraft.references.BlockItemId;

public final class ModBlockItemIds {
    public static final BlockItemId TV = create("tv");
    public static final BlockItemId SPEAKER = create("speaker");

    private ModBlockItemIds() {
    }

    private static BlockItemId create(String name) {
        var id = TVtime.id(name);
        return BlockItemId.create(id, id);
    }
}
