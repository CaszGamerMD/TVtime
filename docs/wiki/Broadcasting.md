# Broadcasting an Application Window

[← Wiki home](Home.md)

TVtime captures a **selected Windows application window** on the broadcaster's Minecraft computer, encodes the video/audio client-side, and sends it to your server for relay to other TVtime clients tuned to the channel.

## Start from the TV Remote

1. Point at a TV Screen and use the TV Remote.
2. Choose a **Channel**, such as \`cinema\`.
3. Select **Broadcast source…**.
4. Select **Quality** and **Codec**.
5. Choose a listed capturable application window to start.
6. Return to the TV settings, verify the channel, and click **Done**.

The window list offers **Refresh** and pagination (five windows per page), as well as a **Stop** control while capture is active.

## Start from chat

\`\`\`mcfunction
/tvtime_capture windows
/tvtime_capture quality BALANCED
/tvtime_capture codec AUTO
/tvtime_capture start 0 cinema
/tvtime_capture status
\`\`\`

Replace the example index \`0\` with an index from your own window list. Tune the screen to \`cinema\` using the remote or \`/tvtime_capture tune cinema\`. To stop: \`/tvtime_capture stop\`.

**Quality or codec changes require stopping the current capture first.**

## Quality presets

| Preset | Video size | Target FPS |
| --- | ---: | ---: |
| LOW | 192 × 108 | 12 |
| BALANCED (**default**) | 320 × 180 | 15 |
| HIGH | 640 × 360 | 20 |

These are small by ordinary video standards because every viewer is decoding/rendering frames inside Minecraft. LOW is useful for diagnosing lag; BALANCED is the recommended starting point.

## Codecs

| Setting | Behavior |
| --- | --- |
| AUTO (**recommended**) | Prefer H.264 and fall back to delta-Deflate when necessary |
| H264 | Select H.264 video |
| DELTA | Select delta-Deflate video |

Development implementation also uses periodic keyframes, media fragmentation/reassembly, Opus for audio where available, and raw PCM fallback. \`/tvtime_capture status\` reports actual codec, profile, FPS, total kbps and video compression ratio.

## Channel behavior

- One active outbound broadcast per player.
- A channel cannot have two simultaneous broadcasters.
- Multiple televisions/speakers can subscribe to one channel.
- Broadcasters must have a supported Windows capture helper; the server does **not** capture the desktop.
- When broadcaster disconnects, its session is removed and subscribers are informed.

## Capture helper and client limitations

The official Windows test-bundle workflow packages \`tvtime-capture.exe\` into the JAR, where it extracts on demand. Other builds may omit that executable. See [Getting Started](Getting-Started.md#installation) for helper paths.

The current backend does not offer Linux/macOS desktop broadcasting. Fullscreen/borderless or protected-content windows may behave differently from ordinary Windows application windows. If a specific application shows black output or cuts off the source, try windowed mode and record the result in an issue.

See [Commands](Commands.md) for the complete command syntax.
