package com.caszgamermd.caszualtvtime.client.screen;

import com.caszgamermd.caszualtvtime.item.PipCorner;
import com.caszgamermd.caszualtvtime.network.payload.ConfigurePortableTvPipPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class PortableTvPipConfigScreen extends Screen {
    private PipCorner corner;

    public PortableTvPipConfigScreen(PipCorner initialCorner) {
        super(Component.literal("Portable TV PiP Settings"));
        this.corner = initialCorner == null
            ? PipCorner.BOTTOM_RIGHT
            : initialCorner;
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;

        addRenderableWidget(
            CycleButton.builder(
                    value -> Component.literal(value.label()),
                    corner
                )
                .withValues(PipCorner.values())
                .create(
                    left,
                    78,
                    200,
                    20,
                    Component.literal("PiP corner"),
                    (button, value) -> corner = value
                )
        );

        addRenderableWidget(
            Button.builder(
                    CommonComponents.GUI_DONE,
                    button -> applyAndClose()
                )
                .bounds(left, 112, 98, 20)
                .build()
        );

        addRenderableWidget(
            Button.builder(
                    CommonComponents.GUI_CANCEL,
                    button -> onClose()
                )
                .bounds(left + 102, 112, 98, 20)
                .build()
        );
    }

    private void applyAndClose() {
        if (ClientPlayNetworking.canSend(
            ConfigurePortableTvPipPayload.TYPE
        )) {
            ClientPlayNetworking.send(
                new ConfigurePortableTvPipPayload(corner)
            );
        }

        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
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
            font,
            title,
            width / 2,
            28,
            -1
        );

        graphics.centeredText(
            font,
            Component.literal(
                "Remote in main hand + Portable TV in offhand"
            ),
            width / 2,
            50,
            0xFFAAAAAA
        );
    }
}
