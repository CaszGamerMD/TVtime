package com.caszgamermd.tvtime.client.hud;

import com.caszgamermd.tvtime.TVtime;
import com.caszgamermd.tvtime.block.ModBlocks;
import com.caszgamermd.tvtime.client.audio.TvAudioAnchors;
import com.caszgamermd.tvtime.client.media.VideoTexture;
import com.caszgamermd.tvtime.client.media.VideoTextureManager;
import com.caszgamermd.tvtime.client.network.ClientChannelDirectory;
import com.caszgamermd.tvtime.client.network.ClientChannelSubscriptions;
import com.caszgamermd.tvtime.item.PortableTvSettings;
import com.caszgamermd.tvtime.audio.SpeakerChannel;

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

    private PortableTvHud() {
    }

    public static void initialize() {
        HudElementRegistry.addLast(
            TVtime.id("portable_tv_pip"),
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
            drawFrame(graphics, MAX_WIDTH, MAX_HEIGHT, channel, false);
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
            drawFrame(graphics, MAX_WIDTH, MAX_HEIGHT, channel, false);
            return;
        }

        int sourceWidth = texture.width();
        int sourceHeight = texture.height();

        float scale = Math.min(
            MAX_WIDTH / (float) sourceWidth,
            MAX_HEIGHT / (float) sourceHeight
        );

        int drawWidth = Math.max(1, Math.round(sourceWidth * scale));
        int drawHeight = Math.max(1, Math.round(sourceHeight * scale));

        int x = graphics.guiWidth() - MARGIN - drawWidth - BORDER * 2;
        int y = graphics.guiHeight() - MARGIN - drawHeight - BORDER * 2 - 22;

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
        boolean live
    ) {
        Minecraft client = Minecraft.getInstance();
        int x = graphics.guiWidth() - MARGIN - width - BORDER * 2;
        int y = graphics.guiHeight() - MARGIN - height - BORDER * 2 - 22;

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
            live ? channel : channel + " • OFFLINE",
            x + BORDER + 5,
            y + BORDER + 5,
            0xFFFFFFFF,
            true
        );
    }
}
