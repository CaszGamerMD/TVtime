package com.caszgamermd.caszualtvtime.block;

import com.caszgamermd.caszualtvtime.audio.SpeakerChannel;
import net.minecraft.core.BlockPos;
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

public final class SpeakerBlockEntity extends BlockEntity {
    private String channel = "";
    private SpeakerChannel speakerChannel = SpeakerChannel.FULL;
    private float volume = 1.0f;
    private int range = 32;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEAKER, pos, state);
    }

    public String channel() {
        return channel;
    }

    public SpeakerChannel speakerChannel() {
        return speakerChannel;
    }

    public float volume() {
        return volume;
    }

    public int range() {
        return range;
    }

    public void configure(String channel, SpeakerChannel speakerChannel, float volume, int range) {
        this.channel = channel == null ? "" : channel.trim();
        this.speakerChannel = speakerChannel == null ? SpeakerChannel.FULL : speakerChannel;
        this.volume = Math.max(0.0f, Math.min(2.0f, volume));
        this.range = Math.max(1, Math.min(128, range));
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putString("channel", channel);
        output.putString("speaker_channel", speakerChannel.name());
        output.putFloat("volume", volume);
        output.putInt("range", range);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        channel = input.getStringOr("channel", "");
        volume = Math.max(0.0f, Math.min(2.0f, input.getFloatOr("volume", 1.0f)));
        range = Math.max(1, Math.min(128, input.getIntOr("range", 32)));
        try {
            speakerChannel = SpeakerChannel.valueOf(
                input.getStringOr("speaker_channel", SpeakerChannel.FULL.name())
            );
        } catch (IllegalArgumentException ignored) {
            speakerChannel = SpeakerChannel.FULL;
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
