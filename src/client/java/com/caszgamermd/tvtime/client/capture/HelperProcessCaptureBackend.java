package com.caszgamermd.tvtime.client.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class HelperProcessCaptureBackend implements WindowCaptureBackend {
    private static final int MEDIA_HEADER_BYTES = 28;
    private static final int MAX_DEBUG_FRAME_BYTES = 16 * 1024 * 1024;

    private final Path executable;

    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private InputStream mediaInput;
    private Thread mediaThread;

    private volatile boolean running;
    private volatile Listener listener;

    public HelperProcessCaptureBackend(Path executable) {
        this.executable = executable.toAbsolutePath().normalize();
    }

    @Override
    public String name() {
        return "Windows Graphics Capture";
    }

    @Override
    public boolean available() {
        return Files.isRegularFile(executable);
    }

    @Override
    public synchronized List<CaptureWindow> listWindows() {
        ensureStarted();

        JsonObject response = request("LIST_WINDOWS", "windows");
        JsonArray windows = response.getAsJsonArray("windows");

        List<CaptureWindow> result = new ArrayList<>(windows.size());
        for (JsonElement element : windows) {
            JsonObject window = element.getAsJsonObject();
            result.add(new CaptureWindow(
                window.get("handle").getAsLong(),
                window.get("processId").getAsInt(),
                string(window, "processName"),
                string(window, "title")
            ));
        }

        return List.copyOf(result);
    }

    @Override
    public synchronized void start(
        CaptureWindow window,
        CaptureOptions options,
        Listener listener
    ) {
        if (running) {
            stop();
        }

        ensureStarted();
        this.listener = listener;

        JsonObject response = request(
            "START " + Long.toUnsignedString(window.nativeHandle()),
            "capture_started"
        );

        int width = response.get("width").getAsInt();
        int height = response.get("height").getAsInt();
        if (width <= 0 || height <= 0) {
            this.listener = null;
            throw new IllegalStateException("Capture helper returned an invalid source size");
        }

        running = true;
    }

    @Override
    public synchronized void stop() {
        if (process == null || !process.isAlive()) {
            cleanup();
            return;
        }

        try {
            if (running) {
                request("STOP", "capture_stopped");
            }
        } catch (RuntimeException ignored) {
        } finally {
            running = false;
            listener = null;
        }
    }

    @Override
    public synchronized boolean running() {
        return running && process != null && process.isAlive();
    }

    public synchronized CaptureStatus status() {
        ensureStarted();
        JsonObject response = request("STATUS", "status");
        return new CaptureStatus(
            response.get("running").getAsBoolean(),
            response.get("frames").getAsLong(),
            response.get("width").getAsInt(),
            response.get("height").getAsInt()
        );
    }

    @Override
    public synchronized void close() {
        if (process == null) {
            return;
        }

        try {
            if (process.isAlive()) {
                try {
                    sendLine("QUIT");
                } catch (IOException ignored) {
                }
                process.destroy();
            }
        } finally {
            cleanup();
        }
    }

    private void ensureStarted() {
        if (process != null && process.isAlive()) {
            return;
        }

        if (!available()) {
            throw new IllegalStateException("TVtime capture helper not found: " + executable);
        }

        try {
            process = new ProcessBuilder(executable.toString()).start();

            reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)
            );
            mediaInput = process.getErrorStream();

            JsonObject hello = readMessage();
            if (!"hello".equals(string(hello, "type"))) {
                throw new IOException("Capture helper did not send a hello message");
            }

            int protocol = hello.get("protocol").getAsInt();
            if (protocol != 1) {
                throw new IOException("Unsupported capture-helper protocol: " + protocol);
            }

            startMediaReader();
        } catch (IOException e) {
            cleanup();
            throw new IllegalStateException("Unable to start TVtime capture helper", e);
        }
    }

    private void startMediaReader() {
        mediaThread = Thread.ofPlatform()
            .name("TVtime-Capture-Media")
            .daemon(true)
            .start(this::mediaLoop);
    }

    private void mediaLoop() {
        byte[] header = new byte[MEDIA_HEADER_BYTES];

        try {
            while (true) {
                if (!readFully(mediaInput, header)) {
                    break;
                }

                if (header[0] != 'T'
                    || header[1] != 'V'
                    || header[2] != 'F'
                    || header[3] != '1') {
                    throw new IOException("Invalid TVtime helper media magic");
                }

                ByteBuffer meta = ByteBuffer
                    .wrap(header)
                    .order(ByteOrder.LITTLE_ENDIAN);

                int type = Byte.toUnsignedInt(header[4]);
                int format = Byte.toUnsignedInt(header[5]);
                int payloadLength = meta.getInt(8);
                long timestampMicros = meta.getLong(12);
                int width = meta.getInt(20);
                int height = meta.getInt(24);

                if (payloadLength < 0 || payloadLength > MAX_DEBUG_FRAME_BYTES) {
                    throw new IOException("Invalid helper media payload length: " + payloadLength);
                }

                byte[] payload = new byte[payloadLength];
                if (!readFully(mediaInput, payload)) {
                    break;
                }

                Listener current = listener;
                if (current == null || !running) {
                    continue;
                }

                if (type == 1 && format == 0 && width > 0 && height > 0) {
                    long expected = (long) width * height * 4L;
                    if (expected != payloadLength) {
                        continue;
                    }

                    ByteBuffer pixels = ByteBuffer.allocateDirect(payloadLength);
                    pixels.put(payload);
                    pixels.flip();

                    current.onVideoFrame(new CapturedVideoFrame(
                        width,
                        height,
                        timestampMicros,
                        CapturedVideoFrame.PixelFormat.BGRA8,
                        pixels
                    ));
                    continue;
                }

                if (type == 2 && format == 0 && width > 0 && height > 0) {
                    if ((payloadLength & 1) != 0) {
                        continue;
                    }

                    ByteBuffer samples = ByteBuffer.allocateDirect(payloadLength);
                    samples.put(payload);
                    samples.flip();

                    current.onAudioChunk(new CapturedAudioChunk(
                        width,
                        height,
                        CapturedAudioChunk.SampleFormat.S16_LE,
                        timestampMicros,
                        samples
                    ));
                }
            }
        } catch (IOException e) {
            if (process != null && process.isAlive()) {
                notifyStopped("Capture media pipe failed: " + e.getMessage());
            }
        }
    }

    private JsonObject request(String command, String expectedType) {
        try {
            sendLine(command);
            JsonObject response = readMessage();

            String type = string(response, "type");
            if ("error".equals(type)) {
                throw new IllegalStateException(
                    string(response, "code") + ": " + string(response, "message")
                );
            }

            if (!expectedType.equals(type)) {
                throw new IllegalStateException(
                    "Expected helper response '" + expectedType + "' but got '" + type + "'"
                );
            }

            return response;
        } catch (IOException e) {
            notifyStopped("Capture helper disconnected: " + e.getMessage());
            cleanup();
            throw new IllegalStateException("Capture helper communication failed", e);
        }
    }

    private void sendLine(String command) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    private JsonObject readMessage() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("capture helper closed stdout");
        }

        JsonElement parsed = JsonParser.parseString(line);
        if (!parsed.isJsonObject()) {
            throw new IOException("capture helper returned a non-object message");
        }

        return parsed.getAsJsonObject();
    }

    private static boolean readFully(InputStream input, byte[] target) throws IOException {
        int offset = 0;
        while (offset < target.length) {
            int read = input.read(target, offset, target.length - offset);
            if (read < 0) {
                return false;
            }
            offset += read;
        }
        return true;
    }

    private void notifyStopped(String reason) {
        Listener current = listener;
        running = false;
        if (current != null) {
            current.onCaptureStopped(reason);
        }
    }

    private void cleanup() {
        running = false;
        listener = null;
        reader = null;
        writer = null;
        mediaInput = null;
        mediaThread = null;

        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    public record CaptureStatus(
        boolean running,
        long frames,
        int width,
        int height
    ) {
    }
}
