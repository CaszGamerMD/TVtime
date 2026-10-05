package com.caszgamermd.tvtime.client.render;

import com.caszgamermd.tvtime.audio.SpeakerChannel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

public final class SpeakerBlockEntityRenderState extends BlockEntityRenderState {
    public BlockPos pos = BlockPos.ZERO;
    public String channel = "";
    public SpeakerChannel speakerChannel = SpeakerChannel.FULL;
    public float volume = 1.0f;
    public int range = 32;
}
