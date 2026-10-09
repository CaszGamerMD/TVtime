# Caszual TV Time Windows capture helper

caszual-tv-time-capture.exe is the Windows-only helper process used by the Minecraft client.

It runs out-of-process rather than through JNI so failures in Windows capture,
hardware video encoding, or audio capture do not crash the JVM.

## Protocol

The helper communicates over stdin/stdout using one-line messages.

Current development commands:

- PING
- LIST_WINDOWS
- START <native-window-handle> (reserved; WGC implementation next)
- QUIT

Responses are one-line JSON objects. On startup the helper emits a hello object
with protocol version 1.

## Native roadmap

1. Enumerate eligible top-level windows. Implemented.
2. Create GraphicsCaptureItem using IGraphicsCaptureItemInterop::CreateForWindow.
3. Create a D3D11 device and free-threaded Direct3D11CaptureFramePool.
4. Copy/scale BGRA frames into an encoder input surface.
5. Capture selected-process audio using WASAPI application/process loopback.
6. Encode video/audio and emit bounded media packets to the Java client.

The Java client owns Minecraft networking. The helper never connects directly
to a Minecraft server.
