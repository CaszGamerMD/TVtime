package com.caszgamermd.tvtime.block;

import com.caszgamermd.tvtime.TVtime;
import net.minecraft.references.BlockItemId;

public final class ModBlockItemIds {
    public static final BlockItemId TV = create("tv");
    public static final BlockItemId CAMERA = create("camera");
    public static final BlockItemId CAMERA_CONTROL_TABLE = create("camera_control_table");
    public static final BlockItemId PORTABLE_TV = create("portable_tv");
    public static final BlockItemId SPEAKER = create("speaker");
    public static final BlockItemId IRON_SPEAKER = create("iron_speaker");
    public static final BlockItemId SPRUCE_SPEAKER = create("spruce_speaker");
    public static final BlockItemId MODERN_SPEAKER = create("modern_speaker");
    public static final BlockItemId CUSTOM_SPEAKER = create("custom_speaker");

    private ModBlockItemIds() {
    }

    private static BlockItemId create(String name) {
        var id = TVtime.id(name);
        return BlockItemId.create(id, id);
    }
}
