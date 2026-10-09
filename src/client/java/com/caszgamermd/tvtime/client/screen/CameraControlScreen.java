package com.caszgamermd.tvtime.client.screen;

import com.caszgamermd.tvtime.network.payload.CameraCatalogPayload;
import com.caszgamermd.tvtime.network.payload.ConfigureCameraPayload;
import com.caszgamermd.tvtime.network.payload.RequestCameraCatalogPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Control table: list/select cameras, edit channel, power, pan, tilt and zoom. */
public final class CameraControlScreen extends Screen {
    private final BlockPos table;
    private List<CameraCatalogPayload.CameraEntry> entries = List.of();
    private int page, selected = -1;
    private boolean editing;
    private String name = "", channel = "";
    private boolean active;
    private float pan, tilt, zoom = 1;
    private EditBox nameBox, channelBox;
    private String status = "Loading camera directory...";

    public CameraControlScreen(BlockPos table) {
        super(Component.literal("TVtime Camera Control Table"));
        this.table = table.immutable();
    }

    public BlockPos tablePos() { return table; }

    public void requestCatalog() {
        if (ClientPlayNetworking.canSend(RequestCameraCatalogPayload.TYPE))
            ClientPlayNetworking.send(new RequestCameraCatalogPayload(table));
    }

    public void updateCatalog(CameraCatalogPayload payload) {
        if (!table.equals(payload.consolePos())) return;
        entries = new ArrayList<>(payload.cameras());
        status = entries.isEmpty() ? "No loaded cameras in this dimension." : entries.size() + " camera(s) found.";
        rebuildWidgets();
    }

    private void edit(int index) {
        if (index < 0 || index >= entries.size()) return;
        selected = index;
        var c = entries.get(index);
        name = c.name();
        channel = c.channel();
        active = c.active();
        pan = c.pan();
        tilt = c.tilt();
        zoom = c.zoom();
        editing = true;
        rebuildWidgets();
    }

    private void rememberFields() {
        if (nameBox != null) name = nameBox.getValue();
        if (channelBox != null) channel = channelBox.getValue();
    }

    private void save() {
        if (selected < 0 || selected >= entries.size()) return;
        if (!ClientPlayNetworking.canSend(ConfigureCameraPayload.TYPE)) {
            status = "Server doesn't accept camera controls.";
            return;
        }
        ClientPlayNetworking.send(new ConfigureCameraPayload(
            table, entries.get(selected).pos(), nameBox.getValue().trim(),
            channelBox.getValue().trim(), active, pan, tilt, zoom
        ));
        editing = false;
        status = "Settings sent to server.";
        requestCatalog();
        rebuildWidgets();
    }

    @Override protected void init() {
        int left = width / 2 - 116;
        if (!editing) {
            int start = page * 6;
            for (int i = start; i < Math.min(entries.size(), start + 6); i++) {
                final int index = i;
                var entry = entries.get(i);
                String label = (entry.active() ? "[ON] " : "[OFF] ") + entry.name();
                if (label.length() > 38) label = label.substring(0, 35) + "...";
                addRenderableWidget(Button.builder(Component.literal(label), b -> edit(index))
                    .bounds(left, 49 + (i - start) * 24, 232, 20).build());
            }
            addRenderableWidget(Button.builder(Component.literal("Previous"), b -> { page = Math.max(0, page-1); rebuildWidgets(); })
                .bounds(left, 206, 75, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> requestCatalog())
                .bounds(left + 79, 206, 75, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Next"), b -> {
                page = Math.min(Math.max(0, (entries.size()-1)/6), page+1); rebuildWidgets();
            }).bounds(left + 158, 206, 74, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(left, 237, 232, 20).build());
            return;
        }

        nameBox = new EditBox(font, left, 50, 232, 20, Component.literal("Camera name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(name);
        addWidget(nameBox);
        channelBox = new EditBox(font, left, 85, 232, 20, Component.literal("TV channel"));
        channelBox.setMaxLength(64);
        channelBox.setValue(channel);
        addWidget(channelBox);

        addRenderableWidget(Button.builder(Component.literal(active ? "Camera ON" : "Camera OFF"),
            b -> { rememberFields(); active = !active; rebuildWidgets(); }).bounds(left, 116, 232, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Pan ◀"), b -> {
            rememberFields(); pan = Math.max(-180, pan - 15); rebuildWidgets();
        }).bounds(left, 144, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Pan ▶"), b -> {
            rememberFields(); pan = Math.min(180, pan + 15); rebuildWidgets();
        }).bounds(left + 140, 144, 92, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Tilt ▲"), b -> {
            rememberFields(); tilt = Math.max(-80, tilt - 10); rebuildWidgets();
        }).bounds(left, 173, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Tilt ▼"), b -> {
            rememberFields(); tilt = Math.min(80, tilt + 10); rebuildWidgets();
        }).bounds(left + 140, 173, 92, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Zoom −"), b -> {
            rememberFields(); zoom = Math.max(1, zoom - 0.5f); rebuildWidgets();
        }).bounds(left, 202, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Zoom +"), b -> {
            rememberFields(); zoom = Math.min(8, zoom + 0.5f); rebuildWidgets();
        }).bounds(left + 140, 202, 92, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
            .bounds(left, 242, 111, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> { editing = false; rebuildWidgets(); })
            .bounds(left + 121, 242, 111, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float pt) {
        super.extractRenderState(graphics, mx, my, pt);
        graphics.centeredText(font, title, width/2, 18, -1);
        if (editing) {
            graphics.text(font, Component.literal("Name"), width/2-115, 38, -6250336);
            graphics.text(font, Component.literal("TV Channel"), width/2-115, 73, -6250336);
            graphics.centeredText(font, Component.literal(String.format("Pan %.0f°", pan)), width/2, 150, -1);
            graphics.centeredText(font, Component.literal(String.format("Tilt %.0f°", tilt)), width/2, 179, -1);
            graphics.centeredText(font, Component.literal(String.format("x%.1f", zoom)), width/2, 208, -1);
            if (nameBox != null) nameBox.extractRenderState(graphics, mx, my, pt);
            if (channelBox != null) channelBox.extractRenderState(graphics, mx, my, pt);
        } else {
            graphics.centeredText(font, Component.literal(status), width/2, 33, -6250336);
            graphics.centeredText(font, Component.literal("Page " + (page+1) + " / " + Math.max(1,(entries.size()+5)/6)),
                width/2, 196, -6250336);
        }
    }

    @Override public void onClose() { minecraft.gui.setScreen(null); }
}
