package com.caszgamermd.tvtime.client.render;

import com.caszgamermd.tvtime.broadcast.DisplayMode;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public final class TvBlockEntityRenderState extends BlockEntityRenderState {
    public boolean anchor;
    public boolean portable;
    public int widthBlocks = 1;
    public int heightBlocks = 1;
    public Direction facing = Direction.NORTH;
    public DisplayMode displayMode = DisplayMode.FIT;
    public String channel = "";
    public boolean tvAudioEnabled = true;
    public BlockPos anchorPos = BlockPos.ZERO;
}
