package com.caszgamermd.tvtime.client.screen;

import com.caszgamermd.tvtime.block.TvBlockEntity;
import com.caszgamermd.tvtime.broadcast.DisplayMode;
import com.caszgamermd.tvtime.network.payload.ConfigureTvPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class TvConfigScreen extends Screen {
    private static final Component CHANNEL_LABEL =
        Component.literal("Channel");

    private final BlockPos pos;
    private final String initialChannel;
    private DisplayMode displayMode;
    private boolean audioEnabled;

    private EditBox channelBox;

    public TvConfigScreen(TvBlockEntity blockEntity) {
        super(Component.literal("TVtime TV Settings"));
        this.pos = blockEntity.getBlockPos().immutable();
        this.initialChannel = blockEntity.channel();
        this.displayMode = blockEntity.displayMode();
        this.audioEnabled = blockEntity.tvAudioEnabled();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;

        this.channelBox = new EditBox(
            this.font,
            left,
            58,
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
                    mode -> Component.literal(mode.name()),
                    displayMode
                )
                .withValues(DisplayMode.values())
                .create(
                    left,
                    96,
                    200,
                    20,
                    Component.literal("Display mode"),
                    (button, value) -> this.displayMode = value
                )
        );

        this.addRenderableWidget(
            CycleButton.onOffBuilder(audioEnabled)
                .create(
                    left,
                    122,
                    200,
                    20,
                    Component.literal("Built-in TV audio"),
                    (button, value) -> this.audioEnabled = value
                )
        );

        this.addRenderableWidget(
            Button.builder(
                    CommonComponents.GUI_DONE,
                    button -> applyAndClose()
                )
                .bounds(left, 160, 98, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(
                    CommonComponents.GUI_CANCEL,
                    button -> onClose()
                )
                .bounds(left + 102, 160, 98, 20)
                .build()
        );
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(channelBox);
    }

    private void applyAndClose() {
        if (ClientPlayNetworking.canSend(ConfigureTvPayload.TYPE)) {
            ClientPlayNetworking.send(
                new ConfigureTvPayload(
                    pos,
                    channelBox.getValue().trim(),
                    displayMode,
                    audioEnabled
                )
            );
        }

        onClose();
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
            20,
            -1
        );

        graphics.text(
            this.font,
            CHANNEL_LABEL,
            this.width / 2 - 99,
            45,
            -6250336
        );

        this.channelBox.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            partialTick
        );
    }
}
