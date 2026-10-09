package com.caszgamermd.caszualtvtime.client.screen;

import com.caszgamermd.caszualtvtime.audio.SpeakerChannel;
import com.caszgamermd.caszualtvtime.block.SpeakerBlockEntity;
import com.caszgamermd.caszualtvtime.network.payload.ConfigureSpeakerPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class SpeakerConfigScreen extends Screen {
    private static final Component CHANNEL_LABEL =
        Component.literal("Channel");
    private static final Component VOLUME_LABEL =
        Component.literal("Volume (0.0 - 2.0)");
    private static final Component RANGE_LABEL =
        Component.literal("Range (1 - 128)");

    private final BlockPos pos;
    private final String initialChannel;
    private final float initialVolume;
    private final int initialRange;
    private SpeakerChannel speakerChannel;

    private EditBox channelBox;
    private EditBox volumeBox;
    private EditBox rangeBox;

    public SpeakerConfigScreen(SpeakerBlockEntity blockEntity) {
        super(Component.literal("Caszual TV Time Speaker Settings"));
        this.pos = blockEntity.getBlockPos().immutable();
        this.initialChannel = blockEntity.channel();
        this.initialVolume = blockEntity.volume();
        this.initialRange = blockEntity.range();
        this.speakerChannel = blockEntity.speakerChannel();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;

        this.channelBox = new EditBox(
            this.font,
            left,
            52,
            200,
            20,
            CHANNEL_LABEL
        );
        this.channelBox.setMaxLength(64);
        this.channelBox.setValue(initialChannel);
        this.channelBox.setHint(Component.literal("broadcast channel"));
        this.addWidget(this.channelBox);

        this.addRenderableWidget(
            CycleButton.builder(
                    role -> Component.literal(role.name()),
                    speakerChannel
                )
                .withValues(SpeakerChannel.values())
                .create(
                    left,
                    84,
                    200,
                    20,
                    Component.literal("Speaker role"),
                    (button, value) -> this.speakerChannel = value
                )
        );

        this.volumeBox = new EditBox(
            this.font,
            left,
            124,
            96,
            20,
            VOLUME_LABEL
        );
        this.volumeBox.setMaxLength(4);
        this.volumeBox.setValue(Float.toString(initialVolume));
        this.addWidget(this.volumeBox);

        this.rangeBox = new EditBox(
            this.font,
            left + 104,
            124,
            96,
            20,
            RANGE_LABEL
        );
        this.rangeBox.setMaxLength(3);
        this.rangeBox.setValue(Integer.toString(initialRange));
        this.addWidget(this.rangeBox);

        this.addRenderableWidget(
            Button.builder(
                    CommonComponents.GUI_DONE,
                    button -> applyAndClose()
                )
                .bounds(left, 172, 98, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(
                    CommonComponents.GUI_CANCEL,
                    button -> onClose()
                )
                .bounds(left + 102, 172, 98, 20)
                .build()
        );
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(channelBox);
    }

    private void applyAndClose() {
        float volume = parseFloat(
            volumeBox.getValue(),
            initialVolume
        );
        int range = parseInt(
            rangeBox.getValue(),
            initialRange
        );

        volume = Math.max(0.0f, Math.min(2.0f, volume));
        range = Math.max(1, Math.min(128, range));

        if (ClientPlayNetworking.canSend(ConfigureSpeakerPayload.TYPE)) {
            ClientPlayNetworking.send(
                new ConfigureSpeakerPayload(
                    pos,
                    channelBox.getValue().trim(),
                    speakerChannel,
                    volume,
                    range
                )
            );
        }

        onClose();
    }

    private static float parseFloat(
        String value,
        float fallback
    ) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(
        String value,
        int fallback
    ) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        super.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            partialTick
        );

        graphics.centeredText(
            this.font,
            this.title,
            this.width / 2,
            18,
            -1
        );

        graphics.text(
            this.font,
            CHANNEL_LABEL,
            this.width / 2 - 99,
            39,
            -6250336
        );

        graphics.text(
            this.font,
            VOLUME_LABEL,
            this.width / 2 - 99,
            112,
            -6250336
        );

        graphics.text(
            this.font,
            RANGE_LABEL,
            this.width / 2 + 5,
            112,
            -6250336
        );

        this.channelBox.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            partialTick
        );
        this.volumeBox.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            partialTick
        );
        this.rangeBox.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            partialTick
        );
    }
}
