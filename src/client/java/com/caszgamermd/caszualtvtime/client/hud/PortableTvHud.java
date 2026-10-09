package com.caszgamermd.caszualcaszual_tv_time.client.hud;

import com.caszgamermd.caszualcaszual_tv_time.CaszualTvTime;
import com.caszgamermd.caszualcaszual_tv_time.audio.SpeakerChannel;
import com.caszgamermd.caszualcaszual_tv_time.block.ModBlocks;
import com.caszgamermd.caszualcaszual_tv_time.client.audio.TvAudioAnchors;
import com.caszgamermd.caszualcaszual_tv_time.client.media.VideoTexture;
import com.caszgamermd.caszualcaszual_tv_time.client.media.VideoTextureManager;
import com.caszgamermd.caszualcaszual_tv_time.client.network.ClientChannelDirectory;
import com.caszgamermd.caszualcaszual_tv_time.client.network.ClientChannelSubscriptions;
import com.caszgamermd.caszualcaszual_tv_time.item.PipCorner;
import com.caszgamermd.caszualcaszual_tv_time.item.PortableTvSettings;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix3x2fStack;

import java.util.UUID;

public final class PortableTvHud {
    private static final int MAX_WIDTH = 160;
    private static final int MAX_HEIGHT = 90;
    private static final int MARGIN = 8;
    private static final int BORDER = 3;
    private static final int LABEL_HEIGHT = 14;
    private static final int BOTTOM_HUD_CLEARANCE = 22;

    private PortableTvHud() {
    }

    public static void initialize() {
        HudElementRegistry.addLast(
            CaszualTvTime.id("portable_tv_pip"),
            PortableTvHud::render
        );
    }

    private static void render(
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        ItemStack stack = client.player.getOffhandItem();
        if (!stack.is(ModBlocks.PORTABLE_TV.asItem())) {
            return;
        }

        var settings = PortableTvSettings.read(stack);
        String channel = settings.channel();
        if (channel.isBlank()) {
            return;
        }

        ClientChannelSubscriptions.markSeen(channel);

        UUID sessionId = ClientChannelDirectory.sessionFor(channel);
        if (sessionId == null) {
            drawFrame(
                graphics,
                MAX_WIDTH,
                MAX_HEIGHT,
                channel,
                settings.pipCorner()
            );
            return;
        }

        if (settings.tvAudioEnabled()) {
            TvAudioAnchors.markSeen(
                sessionId,
                BlockPos.ZERO,
                client.player.position(),
                SpeakerChannel.FULL,
                0.65f,
                18
            );
        }

        VideoTexture texture = VideoTextureManager.get(sessionId);
        texture.updateFromLatest();

        if (!texture.ready()) {
            drawFrame(
                graphics,
                MAX_WIDTH,
                MAX_HEIGHT,
                channel,
                settings.pipCorner()
            );
            return;
        }

        int sourceWidth = texture.width();
        int sourceHeight = texture.height();

        float scale = Math.min(
            MAX_WIDTH / (float) sourceWidth,
            MAX_HEIGHT / (float) sourceHeight
        );

        int drawWidth = Math.max(
            1,
            Math.round(sourceWidth * scale)
        );
        int drawHeight = Math.max(
            1,
            Math.round(sourceHeight * scale)
        );

        Placement placement = placement(
            graphics,
            drawWidth,
            drawHeight,
            settings.pipCorner()
        );

        int x = placement.x();
        int y = placement.y();

        graphics.fill(
            x,
            y,
            x + drawWidth + BORDER * 2,
            y + drawHeight + BORDER * 2,
            0xE0101010
        );

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + BORDER, y + BORDER);
        pose.scale(scale, scale);

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture.textureId(),
            0,
            0,
            0.0f,
            0.0f,
            sourceWidth,
            sourceHeight,
            sourceWidth,
            sourceHeight
        );

        pose.popMatrix();

        graphics.text(
            client.font,
            channel,
            x + BORDER + 3,
            y + drawHeight + BORDER + 5,
            0xFFFFFFFF,
            true
        );
    }

    private static void drawFrame(
        GuiGraphicsExtractor graphics,
        int width,
        int height,
        String channel,
        PipCorner corner
    ) {
        Minecraft client = Minecraft.getInstance();
        Placement placement = placement(
            graphics,
            width,
            height,
            corner
        );

        int x = placement.x();
        int y = placement.y();

        graphics.fill(
            x,
            y,
            x + width + BORDER * 2,
            y + height + BORDER * 2,
            0xE0101010
        );

        graphics.fill(
            x + BORDER,
            y + BORDER,
            x + BORDER + width,
            y + BORDER + height,
            0xFF050505
        );

        graphics.text(
            client.font,
            channel + " • OFFLINE",
            x + BORDER + 5,
            y + BORDER + 5,
            0xFFFFFFFF,
            true
        );
    }

    private static Placement placement(
        GuiGraphicsExtractor graphics,
        int contentWidth,
        int contentHeight,
        PipCorner corner
    ) {
        PipCorner safeCorner = corner == null
            ? PipCorner.BOTTOM_RIGHT
            : corner;

        int totalWidth = contentWidth + BORDER * 2;
        int totalHeight =
            contentHeight + BORDER * 2 + LABEL_HEIGHT;

        int x = switch (safeCorner) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT ->
                graphics.guiWidth() - MARGIN - totalWidth;
        };

        int y = switch (safeCorner) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT ->
                graphics.guiHeight()
                    - MARGIN
                    - BOTTOM_HUD_CLEARANCE
                    - totalHeight;
        };

        return new Placement(
            Math.max(MARGIN, x),
            Math.max(MARGIN, y)
        );
    }

    private record Placement(int x, int y) {
    }
}
