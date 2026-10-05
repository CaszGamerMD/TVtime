package com.caszgamermd.tvtime.client.audio;

import com.caszgamermd.tvtime.client.mixin.SoundEngineAccessor;
import com.caszgamermd.tvtime.client.mixin.SoundManagerAccessor;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TvAudioPlaybackManager {
    private static final float DEFAULT_RANGE = 24.0f;

    private static final Map<UUID, Playback> PLAYBACKS = new HashMap<>();

    private TvAudioPlaybackManager() {
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            clear();
            return;
        }

        Map<UUID, TvAudioAnchors.Anchor> anchors = TvAudioAnchors.active();

        PLAYBACKS.entrySet().removeIf(entry -> {
            if (anchors.containsKey(entry.getKey())) {
                return false;
            }

            entry.getValue().stop();
            return true;
        });

        anchors.forEach((sessionId, anchor) -> {
            Playback playback = PLAYBACKS.get(sessionId);
            if (playback == null || playback.stopped()) {
                playback = create(sessionId);
                if (playback == null) {
                    return;
                }
                PLAYBACKS.put(sessionId, playback);
            }

            playback.update(anchor);
        });
    }

    public static void clear() {
        PLAYBACKS.values().forEach(Playback::stop);
        PLAYBACKS.clear();
        TvAudioAnchors.clear();
    }

    private static Playback create(UUID sessionId) {
        Minecraft client = Minecraft.getInstance();
        SoundManager soundManager = client.getSoundManager();

        SoundEngine soundEngine =
            ((SoundManagerAccessor) soundManager).tvtime$getSoundEngine();

        ChannelAccess channelAccess =
            ((SoundEngineAccessor) soundEngine).tvtime$getChannelAccess();

        ChannelAccess.ChannelHandle handle =
            channelAccess.createHandle(Library.Pool.STREAMING).join();

        if (handle == null) {
            return null;
        }

        TvPcmAudioStream stream = new TvPcmAudioStream(sessionId);

        handle.execute(channel -> {
            channel.setPitch(1.0f);
            channel.setVolume(1.0f);
            channel.linearAttenuation(DEFAULT_RANGE);
            channel.setRelative(false);
            channel.attachBufferStream(stream);
            channel.play();
        });

        return new Playback(handle, stream);
    }

    private record Playback(
        ChannelAccess.ChannelHandle handle,
        TvPcmAudioStream stream
    ) {
        private void update(TvAudioAnchors.Anchor anchor) {
            handle.execute(channel ->
                channel.setSelfPosition(anchor.position())
            );
        }

        private void stop() {
            handle.execute(Channel::stop);
            stream.close();
        }

        private boolean stopped() {
            return handle.isStopped();
        }
    }
}
