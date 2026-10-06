package com.caszgamermd.tvtime.client.screen;

import com.caszgamermd.tvtime.client.capture.CaptureBroadcastController;
import com.caszgamermd.tvtime.client.capture.CaptureProfile;
import com.caszgamermd.tvtime.client.capture.CaptureWindow;
import com.caszgamermd.tvtime.client.media.VideoCodecMode;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CaptureControlScreen extends Screen {
    private static final int MAX_VISIBLE_WINDOWS = 6;

    private final Screen parent;
    private final String suggestedChannel;

    private List<CaptureWindow> windows = List.of();
    private CaptureProfile profile;
    private VideoCodecMode codec;
    private EditBox channelBox;
    private String status = "";

    public CaptureControlScreen(
        Screen parent,
        String suggestedChannel
    ) {
        super(Component.literal("TVtime Broadcast Source"));
        this.parent = parent;
        this.suggestedChannel =
            suggestedChannel == null ? "" : suggestedChannel.trim();

        CaptureBroadcastController controller =
            CaptureBroadcastController.instance();

        this.profile = controller.captureProfile();
        this.codec = controller.preferredCodec();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 120;

        this.channelBox = new EditBox(
            this.font,
            left,
            44,
            240,
            20,
            Component.literal("Broadcast channel")
        );
        this.channelBox.setMaxLength(64);

        CaptureBroadcastController controller =
            CaptureBroadcastController.instance();

        String activeChannel = controller.channel();
        this.channelBox.setValue(
            activeChannel != null && !activeChannel.isBlank()
                ? activeChannel
                : suggestedChannel
        );
        this.channelBox.setHint(Component.literal("broadcast channel"));
        this.addWidget(this.channelBox);

        this.addRenderableWidget(
            CycleButton.builder(
                    value -> Component.literal(value.name()),
                    profile
                )
                .withValues(CaptureProfile.values())
                .create(
                    left,
                    70,
                    118,
                    20,
                    Component.literal("Quality"),
                    (button, value) -> this.profile = value
                )
        );

        this.addRenderableWidget(
            CycleButton.builder(
                    value -> Component.literal(value.name()),
                    codec
                )
                .withValues(VideoCodecMode.values())
                .create(
                    left + 122,
                    70,
                    118,
                    20,
                    Component.literal("Codec"),
                    (button, value) -> this.codec = value
                )
        );

        loadWindows();

        int y = 106;
        int count = Math.min(MAX_VISIBLE_WINDOWS, windows.size());

        for (int i = 0; i < count; i++) {
            final int windowIndex = i;
            CaptureWindow window = windows.get(i);

            this.addRenderableWidget(
                Button.builder(
                        Component.literal(
                            "[" + i + "] " + window.displayName()
                        ),
                        button -> start(windowIndex)
                    )
                    .bounds(left, y, 240, 20)
                    .build()
            );

            y += 22;
        }

        if (controller.running()) {
            this.addRenderableWidget(
                Button.builder(
                        Component.literal("Stop Broadcast"),
                        button -> {
                            controller.stop();
                            status = "Broadcast stopped.";
                        }
                    )
                    .bounds(left, 264, 118, 20)
                    .build()
            );
        }

        this.addRenderableWidget(
            Button.builder(
                    Component.literal("Back"),
                    button -> onClose()
                )
                .bounds(
                    controller.running() ? left + 122 : left,
                    264,
                    controller.running() ? 118 : 240,
                    20
                )
                .build()
        );
    }

    private void loadWindows() {
        try {
            windows = CaptureBroadcastController.instance().windows();
            status = windows.isEmpty()
                ? "No capturable windows found."
                : "Choose a window to begin broadcasting.";
        } catch (RuntimeException ex) {
            windows = List.of();
            status = "Capture unavailable: " + ex.getMessage();
        }
    }

    private void start(int windowIndex) {
        String channel = channelBox.getValue().trim();
        if (channel.isEmpty()) {
            status = "Enter a broadcast channel first.";
            return;
        }

        CaptureBroadcastController controller =
            CaptureBroadcastController.instance();

        try {
            if (controller.running()) {
                controller.stop();
            }

            controller.setCaptureProfile(profile);
            controller.setPreferredCodec(codec);
            controller.start(windowIndex, channel);

            status = "Broadcast requested on channel '" + channel + "'.";
        } catch (RuntimeException ex) {
            status = "Unable to start: " + ex.getMessage();
        }
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(channelBox);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
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
            Component.literal("Channel"),
            this.width / 2 - 119,
            32,
            -6250336
        );

        if (!status.isBlank()) {
            graphics.centeredText(
                this.font,
                Component.literal(status),
                this.width / 2,
                248,
                -1
            );
        }

        if (windows.size() > MAX_VISIBLE_WINDOWS) {
            graphics.centeredText(
                this.font,
                Component.literal(
                    "Showing first " + MAX_VISIBLE_WINDOWS
                        + " of " + windows.size()
                        + " windows. Use /tvtime_capture windows for the full list."
                ),
                this.width / 2,
                232,
                -6250336
            );
        }

        this.channelBox.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            partialTick
        );
    }
}
