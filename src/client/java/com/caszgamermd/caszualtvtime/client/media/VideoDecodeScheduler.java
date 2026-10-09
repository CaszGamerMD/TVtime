package com.caszgamermd.caszualtvtime.client.media;

import com.caszgamermd.caszualtvtime.network.MediaKind;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps expensive video decoding off Fabric's networking callback.
 *
 * Each session is decoded serially so inter-frame codec state remains valid,
 * while different broadcasts can decode in parallel. Queues are deliberately
 * bounded: if a client cannot keep up, CaszualTvTime skips forward to the newest
 * available keyframe instead of building seconds of latency.
 */
public final class VideoDecodeScheduler {
    private static final int WORKERS = Math.max(
        2,
        Math.min(4, Runtime.getRuntime().availableProcessors() / 2)
    );
    private static final int MAX_QUEUED_FRAMES = 8;

    private static final ExecutorService EXECUTOR =
        Executors.newFixedThreadPool(
            WORKERS,
            runnable -> {
                Thread thread = new Thread(runnable, "Caszual TV Time-Video-Decode");
                thread.setDaemon(true);
                return thread;
            }
        );

    private static final Map<UUID, SessionQueue> QUEUES =
        new ConcurrentHashMap<>();

    private VideoDecodeScheduler() {
    }

    public static void submit(
        UUID sessionId,
        long sequence,
        long presentationTimeMicros,
        boolean keyFrame,
        byte[] encoded
    ) {
        QUEUES.computeIfAbsent(
            sessionId,
            SessionQueue::new
        ).offer(new Job(
            sequence,
            presentationTimeMicros,
            keyFrame,
            encoded
        ));
    }

    public static void remove(UUID sessionId) {
        SessionQueue queue = QUEUES.remove(sessionId);
        if (queue != null) {
            queue.clear();
        }
    }

    public static void clear() {
        QUEUES.values().forEach(SessionQueue::clear);
        QUEUES.clear();
    }

    private static DecodedVideoFrame decode(
        UUID sessionId,
        Job job
    ) {
        byte[] encoded = job.encoded();

        if (H264VideoCodec.isH264Packet(encoded)) {
            return H264VideoDecoderStore.decode(
                sessionId,
                job.sequence(),
                job.presentationTimeMicros(),
                job.keyFrame(),
                encoded
            );
        }

        DecodedVideoFrame delta =
            DeltaVideoDecoderStore.decode(
                sessionId,
                job.sequence(),
                job.presentationTimeMicros(),
                job.keyFrame(),
                encoded
            );

        if (delta != null) {
            return delta;
        }

        DecodedVideoFrame compressed =
            DeflateVideoCodec.decode(
                encoded,
                job.presentationTimeMicros()
            );

        if (compressed != null) {
            return compressed;
        }

        return RawTestFrameCodec.decode(
            encoded,
            job.presentationTimeMicros()
        );
    }

    private static final class SessionQueue {
        private final UUID sessionId;
        private final ArrayDeque<Job> jobs = new ArrayDeque<>();
        private boolean running;

        private SessionQueue(UUID sessionId) {
            this.sessionId = sessionId;
        }

        private synchronized void offer(Job job) {
            if (jobs.size() >= MAX_QUEUED_FRAMES) {
                jobs.clear();

                // Inter-frame state is no longer trustworthy after dropping
                // queued frames. Reset codec history and only resume
                // immediately if this packet itself is a keyframe.
                H264VideoDecoderStore.remove(sessionId);
                DeltaVideoDecoderStore.remove(sessionId);

                if (!job.keyFrame()) {
                    return;
                }
            }

            jobs.addLast(job);

            if (!running) {
                running = true;
                EXECUTOR.execute(this::drain);
            }
        }

        private void drain() {
            while (true) {
                Job job;

                synchronized (this) {
                    job = jobs.pollFirst();
                    if (job == null) {
                        running = false;
                        return;
                    }
                }

                DecodedVideoFrame frame = decode(sessionId, job);
                if (frame != null) {
                    VideoFrameStore.publish(sessionId, frame);
                }
            }
        }

        private synchronized void clear() {
            jobs.clear();
            running = false;
        }
    }

    private record Job(
        long sequence,
        long presentationTimeMicros,
        boolean keyFrame,
        byte[] encoded
    ) {
    }
}
