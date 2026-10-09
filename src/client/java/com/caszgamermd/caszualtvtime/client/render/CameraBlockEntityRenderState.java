package com.caszgamermd.caszualtvtime.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** The camera is physically fixed, while its visual head follows the viewing angles. */
public final class CameraBlockEntityRenderState extends BlockEntityRenderState {
    public float yaw;
    public float tilt;
}
