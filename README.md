# TVtime

TVtime is a Fabric mod for Minecraft 26.2 that aims to let players broadcast a selected desktop application window, including audio, to shared in-world TV displays.

## Planned architecture

- **Client capture:** a broadcasting player's client captures a selected window and its application audio.
- **Client encode:** video/audio are encoded client-side.
- **Server relay:** the dedicated server owns session state and relays encoded media packets; it does not render or transcode media.
- **Client decode/render:** viewing clients decode the stream and render it across connected TV blocks.
- **Positional audio:** TV blocks and linked speaker blocks act as in-world sound emitters.

## V1 targets

- Connected TV blocks form one seamless logical display.
- Fit / Fill / Stretch presentation modes.
- Server-authoritative broadcast sessions and channel assignment.
- Multiple displays can tune to the same channel.
- TV-local positional audio.
- Linkable speaker blocks with Full / Left / Right roles.
- Configurable server limits for stream resolution, FPS, and bitrate.
- Windows-first application capture.
- Dedicated-server-safe class separation.

## Current status

Initial Fabric 26.2 project and core domain model are being built.
