package com.caszgamermd.caszualtvtime.item;

import com.caszgamermd.caszualtvtime.block.TvBlockEntity;
import com.caszgamermd.caszualtvtime.broadcast.DisplayMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PortableTvSettings {
    private static final String CHANNEL = "caszual_tv_time_channel";
    private static final String DISPLAY_MODE = "caszual_tv_time_display_mode";
    private static final String AUDIO = "caszual_tv_time_audio";
    private static final String PIP_CORNER = "caszual_tv_time_pip_corner";

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

        PipCorner corner;
        try {
            corner = PipCorner.valueOf(
                tag.getStringOr(
                    PIP_CORNER,
                    PipCorner.BOTTOM_RIGHT.name()
                )
            );
        } catch (IllegalArgumentException ignored) {
            corner = PipCorner.BOTTOM_RIGHT;
        }

        return new Settings(channel, mode, audio, corner);
    }

    public static void copyFrom(ItemStack stack, TvBlockEntity tv) {
        Settings current = read(stack);

        write(
            stack,
            new Settings(
                tv.channel(),
                tv.displayMode(),
                tv.tvAudioEnabled(),
                current.pipCorner()
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
                tag.putString(PIP_CORNER, safe.pipCorner().name());
            }
        );
    }

    public static void setPipCorner(
        ItemStack stack,
        PipCorner corner
    ) {
        Settings current = read(stack);
        write(
            stack,
            new Settings(
                current.channel(),
                current.displayMode(),
                current.tvAudioEnabled(),
                corner
            )
        );
    }

    public record Settings(
        String channel,
        DisplayMode displayMode,
        boolean tvAudioEnabled,
        PipCorner pipCorner
    ) {
        public static final Settings DEFAULT =
            new Settings(
                "",
                DisplayMode.FIT,
                true,
                PipCorner.BOTTOM_RIGHT
            );

        public Settings {
            channel = channel == null ? "" : channel.trim();
            displayMode = displayMode == null
                ? DisplayMode.FIT
                : displayMode;
            pipCorner = pipCorner == null
                ? PipCorner.BOTTOM_RIGHT
                : pipCorner;
        }
    }
}
