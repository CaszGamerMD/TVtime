package com.caszgamermd.tvtime.item;

import com.caszgamermd.tvtime.block.TvBlockEntity;
import com.caszgamermd.tvtime.broadcast.DisplayMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PortableTvSettings {
    private static final String CHANNEL = "tvtime_channel";
    private static final String DISPLAY_MODE = "tvtime_display_mode";
    private static final String AUDIO = "tvtime_audio";

    private PortableTvSettings() {
    }

    public static Settings read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Settings.DEFAULT;
        }

        CompoundTag tag = data.copyTag();
        String channel = tag.getStringOr(CHANNEL, "");
        boolean audio = tag.getBooleanOr(AUDIO, true);

        DisplayMode mode;
        try {
            mode = DisplayMode.valueOf(
                tag.getStringOr(DISPLAY_MODE, DisplayMode.FIT.name())
            );
        } catch (IllegalArgumentException ignored) {
            mode = DisplayMode.FIT;
        }

        return new Settings(channel, mode, audio);
    }

    public static void copyFrom(ItemStack stack, TvBlockEntity tv) {
        write(
            stack,
            new Settings(
                tv.channel(),
                tv.displayMode(),
                tv.tvAudioEnabled()
            )
        );
    }

    public static void write(ItemStack stack, Settings settings) {
        Settings safe = settings == null ? Settings.DEFAULT : settings;

        CustomData.update(
            DataComponents.CUSTOM_DATA,
            stack,
            tag -> {
                tag.putString(CHANNEL, safe.channel());
                tag.putString(DISPLAY_MODE, safe.displayMode().name());
                tag.putBoolean(AUDIO, safe.tvAudioEnabled());
            }
        );
    }

    public record Settings(
        String channel,
        DisplayMode displayMode,
        boolean tvAudioEnabled
    ) {
        public static final Settings DEFAULT =
            new Settings("", DisplayMode.FIT, true);

        public Settings {
            channel = channel == null ? "" : channel.trim();
            displayMode = displayMode == null
                ? DisplayMode.FIT
                : displayMode;
        }
    }
}
