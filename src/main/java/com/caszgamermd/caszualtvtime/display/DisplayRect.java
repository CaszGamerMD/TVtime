package com.caszgamermd.caszualtvtime.display;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record DisplayRect(
    BlockPos anchor,
    int widthBlocks,
    int heightBlocks,
    Direction facing
) {
    public int blockCount() {
        return widthBlocks * heightBlocks;
    }
}
