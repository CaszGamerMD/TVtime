package com.caszgamermd.tvtime.block;

import com.caszgamermd.tvtime.broadcast.DisplayMode;
import com.caszgamermd.tvtime.display.DisplayRect;
import com.caszgamermd.tvtime.display.TvDisplayScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class TvBlockEntity extends BlockEntity {
    private String channel = "";
    private DisplayMode displayMode = DisplayMode.FIT;
    private boolean tvAudioEnabled = true;

    public TvBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TV, pos, state);
    }

    public String channel() {
        return channel;
    }

    public DisplayMode displayMode() {
        return displayMode;
    }

    public boolean tvAudioEnabled() {
        return tvAudioEnabled;
    }

    public DisplayRect displayRect() {
        Direction fallbackFacing = getBlockState().hasProperty(TvBlock.FACING)
            ? getBlockState().getValue(TvBlock.FACING)
            : Direction.NORTH;

        if (level == null || !level.getBlockState(worldPosition).is(ModBlocks.TV)) {
            return new DisplayRect(
                worldPosition.immutable(),
                1,
                1,
                fallbackFacing
            );
        }

        return TvDisplayScanner.scan(level, worldPosition);
    }

    public void setChannel(String channel) {
        this.channel = channel == null ? "" : channel.trim();
        setChanged();
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode == null ? DisplayMode.FIT : displayMode;
        setChanged();
    }

    public void setTvAudioEnabled(boolean enabled) {
        this.tvAudioEnabled = enabled;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putString("channel", channel);
        output.putString("display_mode", displayMode.name());
        output.putBoolean("tv_audio_enabled", tvAudioEnabled);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        channel = input.getStringOr("channel", "");
        tvAudioEnabled = input.getBooleanOr("tv_audio_enabled", true);
        try {
            displayMode = DisplayMode.valueOf(input.getStringOr("display_mode", DisplayMode.FIT.name()));
        } catch (IllegalArgumentException ignored) {
            displayMode = DisplayMode.FIT;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }
}
