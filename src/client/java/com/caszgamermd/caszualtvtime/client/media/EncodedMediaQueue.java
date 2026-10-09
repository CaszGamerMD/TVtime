package com.caszgamermd.caszualcaszual_tv_time.client.media;

import com.caszgamermd.caszualcaszual_tv_time.network.MediaChunk;
import com.caszgamermd.caszualcaszual_tv_time.network.MediaKind;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded receive queue for one broadcast session.
 *
 * Video favors low latency: when overloaded, old non-keyframe video is dropped.
 * Audio has its own byte budget so a video burst cannot consume all queue space.
 */
public final class EncodedMediaQueue {
    private final long maxVideoBytes;
    private final long maxAudioBytes;

    private final Deque<MediaChunk> video = new ArrayDeque<>();
    private final Deque<MediaChunk> audio = new ArrayDeque<>();

    private long videoBytes;
    private long audioBytes;

    public EncodedMediaQueue(long maxVideoBytes, long maxAudioBytes) {
        if (maxVideoBytes <= 0 || maxAudioBytes <= 0) {
            throw new IllegalArgumentException("Queue budgets must be positive");
        }
        this.maxVideoBytes = maxVideoBytes;
        this.maxAudioBytes = maxAudioBytes;
    }

    public synchronized void offer(MediaChunk chunk) {
        if (chunk.kind() == MediaKind.VIDEO) {
            offerVideo(chunk);
        } else {
            offerAudio(chunk);
        }
    }

    public synchronized MediaChunk pollVideo() {
        MediaChunk chunk = video.pollFirst();
        if (chunk != null) {
            videoBytes -= chunk.payload().length;
        }
        return chunk;
    }

    public synchronized MediaChunk pollAudio() {
        MediaChunk chunk = audio.pollFirst();
        if (chunk != null) {
            audioBytes -= chunk.payload().length;
        }
        return chunk;
    }

    public synchronized void clear() {
        video.clear();
        audio.clear();
        videoBytes = 0;
        audioBytes = 0;
    }

    public synchronized int videoPackets() {
        return video.size();
    }

    public synchronized int audioPackets() {
        return audio.size();
    }

    private void offerVideo(MediaChunk chunk) {
        int bytes = chunk.payload().length;
        if (bytes > maxVideoBytes) {
            return;
        }

        while (!video.isEmpty() && videoBytes + bytes > maxVideoBytes) {
            MediaChunk removed = video.removeFirst();
            videoBytes -= removed.payload().length;
        }

        // If this is a keyframe, everything older is useless for starting fresh.
        if (chunk.keyFrame()) {
            video.clear();
            videoBytes = 0;
        }

        video.addLast(chunk);
        videoBytes += bytes;
    }

    private void offerAudio(MediaChunk chunk) {
        int bytes = chunk.payload().length;
        if (bytes > maxAudioBytes) {
            return;
        }

        while (!audio.isEmpty() && audioBytes + bytes > maxAudioBytes) {
            MediaChunk removed = audio.removeFirst();
            audioBytes -= removed.payload().length;
        }

        audio.addLast(chunk);
        audioBytes += bytes;
    }
}
