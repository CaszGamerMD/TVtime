package com.caszgamermd.caszualcaszual_tv_time.client.audio;

import com.caszgamermd.caszualcaszual_tv_time.audio.SpeakerChannel;
import com.caszgamermd.caszualcaszual_tv_time.client.mixin.SoundEngineAccessor;
import com.caszgamermd.caszualcaszual_tv_time.client.mixin.SoundManagerAccessor;
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
    private static final Map<TvAudioAnchors.SourceKey, Playback> PLAYBACKS =
        new HashMap<>();

    private TvAudioPlaybackManager() {
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            clear();
            return;
        }

        Map<TvAudioAnchors.SourceKey, TvAudioAnchors.Anchor> anchors =
            TvAudioAnchors.active();

        PLAYBACKS.entrySet().removeIf(entry -> {
            if (anchors.containsKey(entry.getKey())) {
                return false;
            }

            entry.getValue().stop();
            return true;
        });

        Map<RoleKey, Integer> duplicateCounts = new HashMap<>();
        anchors.forEach((sourceKey, anchor) ->
            duplicateCounts.merge(
                new RoleKey(
                    sourceKey.sessionId(),
                    anchor.speakerChannel()
                ),
                1,
                Integer::sum
            )
        );

        anchors.forEach((sourceKey, anchor) -> {
            Playback playback = PLAYBACKS.get(sourceKey);

            if (playback != null
                && playback.speakerChannel() != anchor.speakerChannel()) {
                playback.stop();
                PLAYBACKS.remove(sourceKey);
                playback = null;
            }

            if (playback == null || playback.stopped()) {
                playback = create(sourceKey, anchor.speakerChannel());
                if (playback == null) {
                    return;
                }
                PLAYBACKS.put(sourceKey, playback);
            }

            int duplicateCount = duplicateCounts.getOrDefault(
                new RoleKey(
                    sourceKey.sessionId(),
                    anchor.speakerChannel()
                ),
                1
            );

            playback.update(
                anchor,
                1.0f / Math.max(1, duplicateCount)
            );
        });
    }

    public static void clear() {
        PLAYBACKS.values().forEach(Playback::stop);
        PLAYBACKS.clear();
        TvAudioAnchors.clear();
    }

    private static Playback create(
        TvAudioAnchors.SourceKey sourceKey,
        SpeakerChannel speakerChannel
    ) {
        Minecraft client = Minecraft.getInstance();
        SoundManager soundManager = client.getSoundManager();

        SoundEngine soundEngine =
            ((SoundManagerAccessor) soundManager).caszual_tv_time$getSoundEngine();

        ChannelAccess channelAccess =
            ((SoundEngineAccessor) soundEngine).caszual_tv_time$getChannelAccess();

        ChannelAccess.ChannelHandle handle =
            channelAccess.createHandle(Library.Pool.STREAMING).join();

        if (handle == null) {
            return null;
        }

        TvPcmAudioStream stream =
            new TvPcmAudioStream(
                sourceKey.sessionId(),
                speakerChannel
            );

        handle.execute(channel -> {
            channel.setPitch(1.0f);
            channel.setVolume(1.0f);
            channel.linearAttenuation(24.0f);
            channel.setRelative(false);
            channel.attachBufferStream(stream);
            channel.play();
        });

        return new Playback(handle, stream, speakerChannel);
    }

    private record RoleKey(
        UUID sessionId,
        SpeakerChannel speakerChannel
    ) {
    }

    private record Playback(
        ChannelAccess.ChannelHandle handle,
        TvPcmAudioStream stream,
        SpeakerChannel speakerChannel
    ) {
        private void update(
            TvAudioAnchors.Anchor anchor,
            float normalization
        ) {
            handle.execute(channel -> {
                channel.setSelfPosition(anchor.position());
                channel.setVolume(
                    anchor.volume() * normalization
                );
                channel.linearAttenuation(anchor.range());
            });
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
