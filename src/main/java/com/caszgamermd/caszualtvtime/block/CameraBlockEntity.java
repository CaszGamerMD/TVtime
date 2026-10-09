package com.caszgamermd.caszualcaszual_tv_time.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;

public final class CameraBlockEntity extends BlockEntity {
    private String name = "";
    private String channel = "";
    private boolean active = false;
    private float pan = 0.0f;
    private float tilt = 0.0f;
    private float zoom = 1.0f;

    public CameraBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMERA, pos, state);
    }

    public String cameraName() {
        return name.isBlank() ? "Camera " + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ() : name;
    }
    public String channel() { return channel; }
    public boolean active() { return active; }
    public float pan() { return pan; }
    public float tilt() { return tilt; }
    public float zoom() { return zoom; }

    public void configure(String name, String channel, boolean active, float pan, float tilt, float zoom) {
        this.name = name == null ? "" : name.trim().substring(0, Math.min(32, name.trim().length()));
        String rawChannel = channel == null ? "" : channel.trim();
        this.channel = rawChannel.substring(0, Math.min(64, rawChannel.length()));
        this.active = active;
        this.pan = Float.isFinite(pan) ? Math.max(-180.0f, Math.min(180.0f, pan)) : 0.0f;
        this.tilt = Float.isFinite(tilt) ? Math.max(-80.0f, Math.min(80.0f, tilt)) : 0.0f;
        this.zoom = Float.isFinite(zoom) ? Math.max(1.0f, Math.min(8.0f, zoom)) : 1.0f;
        setChanged();
    }

    public float yawDegrees() {
        Direction facing = getBlockState().getValue(TvBlock.FACING);
        return facing.toYRot() + pan;
    }

    @Override protected void saveAdditional(ValueOutput output) {
        output.putString("name", name);
        output.putString("channel", channel);
        output.putBoolean("active", active);
        output.putFloat("pan", pan);
        output.putFloat("tilt", tilt);
        output.putFloat("zoom", zoom);
        super.saveAdditional(output);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        name = input.getStringOr("name", "");
        channel = input.getStringOr("channel", "");
        active = input.getBooleanOr("active", false);
        pan = input.getFloatOr("pan", 0.0f);
        tilt = input.getFloatOr("tilt", 0.0f);
        zoom = input.getFloatOr("zoom", 1.0f);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookups) { return saveWithoutMetadata(lookups); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void setChanged() {
        super.setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }
}
