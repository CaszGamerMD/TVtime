package com.caszgamermd.caszualtvtime.client.screen;

import com.caszgamermd.caszualtvtime.client.capture.CaptureBroadcastController;
import com.caszgamermd.caszualtvtime.client.capture.CaptureProfile;
import com.caszgamermd.caszualtvtime.client.capture.CaptureWindow;
import com.caszgamermd.caszualtvtime.client.media.VideoCodecMode;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CaptureControlScreen extends Screen {
    private static final int MAX_VISIBLE_WINDOWS = 5;

    private final Screen parent;
    private final String suggestedChannel;

    private List<CaptureWindow> windows = List.of();
    private CaptureProfile profile;
    private VideoCodecMode codec;
    private EditBox channelBox;
    private String channelValue;
    private String status = "";
    private int page;
    private boolean windowsLoaded;

    public CaptureControlScreen(
        Screen parent,
        String suggestedChannel
    ) {
        super(Component.literal("Caszual TV Time Broadcast Source"));
        this.parent = parent;
        this.suggestedChannel =
            suggestedChannel == null ? "" : suggestedChannel.trim();
        this.channelValue = this.suggestedChannel;

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
                : channelValue
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

        if (!windowsLoaded) {
            loadWindows();
            windowsLoaded = true;
        }

        int pageCount = Math.max(
            1,
            (windows.size() + MAX_VISIBLE_WINDOWS - 1)
                / MAX_VISIBLE_WINDOWS
        );
        page = Math.max(0, Math.min(page, pageCount - 1));

        int startIndex = page * MAX_VISIBLE_WINDOWS;
        int endIndex = Math.min(
            windows.size(),
            startIndex + MAX_VISIBLE_WINDOWS
        );

        int y = 106;

        for (int i = startIndex; i < endIndex; i++) {
            final int windowIndex = i;
            CaptureWindow window = windows.get(i);

            this.addRenderableWidget(
                Button.builder(
                        Component.literal(
                            "[" + i + "] " + shortName(window.displayName())
                        ),
                        button -> start(windowIndex)
                    )
                    .bounds(left, y, 240, 20)
                    .build()
            );

            y += 22;
        }

        this.addRenderableWidget(
            Button.builder(
                    Component.literal("< Prev"),
                    button -> changePage(-1)
                )
                .bounds(left, 218, 76, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(
                    Component.literal(
                        (page + 1) + " / " + pageCount
                    ),
                    button -> {
                    }
                )
                .bounds(left + 82, 218, 76, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(
                    Component.literal("Next >"),
                    button -> changePage(1)
                )
                .bounds(left + 164, 218, 76, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(
                    Component.literal("Refresh"),
                    button -> refreshWindows()
                )
                .bounds(left, 242, 76, 20)
                .build()
        );

        if (controller.running()) {
            this.addRenderableWidget(
                Button.builder(
                        Component.literal("Stop"),
                        button -> {
                            controller.stop();
                            status = "Broadcast stopped.";
                        }
                    )
                    .bounds(left + 82, 242, 76, 20)
                    .build()
            );
        }

        this.addRenderableWidget(
            Button.builder(
                    Component.literal("Back"),
                    button -> onClose()
                )
                .bounds(
                    controller.running() ? left + 164 : left + 82,
                    242,
                    controller.running() ? 76 : 158,
                    20
                )
                .build()
        );
    }

    private void changePage(int delta) {
        channelValue = channelBox.getValue();
        page += delta;
        rebuildWidgets();
    }

    private void refreshWindows() {
        channelValue = channelBox.getValue();
        page = 0;
        loadWindows();
        windowsLoaded = true;
        rebuildWidgets();
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

    private static String shortName(String value) {
        if (value == null || value.isBlank()) {
            return "(untitled window)";
        }

        String normalized = value.trim();
        return normalized.length() <= 42
            ? normalized
            : normalized.substring(0, 39) + "...";
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

            channelValue = channel;
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
                278,
                -1
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
