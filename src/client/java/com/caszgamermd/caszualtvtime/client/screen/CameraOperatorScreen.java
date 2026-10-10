package com.caszgamermd.caszualtvtime.client.screen;

import com.caszgamermd.caszualtvtime.client.camera.CameraOperator;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Transparent camera operator screen: no buttons or overlay in captured video. */
public final class CameraOperatorScreen extends Screen {
    public CameraOperatorScreen() {
        super(Component.literal("Caszual TV Time HD Camera — Esc to stop"));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        CameraOperator.stop();
        minecraft.gui.setScreen(null);
    }

    @Override public void removed() {
        CameraOperator.stop();
        super.removed();
    }
}
