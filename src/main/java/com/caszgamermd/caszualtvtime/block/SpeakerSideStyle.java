package com.caszgamermd.caszualcaszual_tv_time.block;

import net.minecraft.util.StringRepresentable;

public enum SpeakerSideStyle implements StringRepresentable {
    IRON("iron"),
    SPRUCE("spruce"),
    BLACK("black"),
    WHITE("white"),
    COPPER("copper"),
    STONE("stone");

    private final String serializedName;

    SpeakerSideStyle(String serializedName) {
        this.serializedName = serializedName;
    }

    public SpeakerSideStyle next() {
        SpeakerSideStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
