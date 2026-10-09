# Technical Notes (Developers & Server Admins)

[← Wiki home](Home.md)

## Architecture

1. **Broadcaster client:** uses a separate native **Windows** helper process to list/capture an application window and its selected-process audio.
2. **Client encoder:** prepares video (H.264 or delta-Deflate) and audio (Opus, with PCM fallback).
3. **Minecraft server:** owns channel/session state, validates streaming limits, and relays bounded media packets to subscribed players. The server does not decode the desktop stream or transcode media.
4. **Viewing clients:** subscribe to channels, decode video frames, upload/update textures, render images on the connected TV surfaces or portable overlay, and play positional sound.

The JAR has split main/client Fabric entrypoints so dedicated server code does not depend on client rendering classes.

## Limits in the code

| Setting | Current implementation |
| --- | --- |
| Minecraft | 26.2 |
| Java target | 25 |
| Fabric Loader | >=0.19.3 |
| Default video max (server) | 1280 × 720 |
| Max server stream FPS | 30 |
| Max declared video bitrate | 4,000 kbps |
| Default audio bitrate limit record | 160 kbps |
| Connected display bounds | 16 × 16 TV blocks |
| Speaker range | 1–128 blocks |
| Speaker volume | 0.0–2.0 |
| One broadcaster | At most one outbound channel per player |
| Channel occupancy | One broadcaster per channel |

The \`StreamLimits\` default values are **code defaults**, not evidence of a configurable server settings screen/file. The shipped quality presets are much lower than the maximum server limits.

## Project layout

| Directory/file | Purpose |
| --- | --- |
| \`src/main/java/.../block/\` | Block registration, state, block entities |
| \`src/main/java/.../broadcast/\` | Channel sessions and state |
| \`src/main/java/.../network/\` | Network payload registration and server relay |
| \`src/main/java/.../display/\` | Connected display scanning and layout |
| \`src/client/java/.../capture/\` | Client capture backends and controller |
| \`src/client/java/.../media/\` | Encoders, decoders, packets, texture management |
| \`src/client/java/.../render/\` | TV and speaker rendering |
| \`src/client/java/.../screen/\` | Remote configuration and broadcast GUI |
| \`src/client/java/.../hud/\` | Offhand Portable TV picture-in-picture |
| \`native/windows/\` | Windows capture helper source |
| \`src/main/resources/data/tvtime/recipe/\` | Actual crafting JSON files |
| \`dist/\` | Published alpha test JAR/instructions |

## Build

The repository uses Fabric Loom, Gradle **9.8.0**, and Java **25**. The normal GitHub Actions build runs \`gradle build --stacktrace\`, producing JARs in \`build/libs/\`.

The **Windows Test Bundle** workflow builds the Windows helper with CMake, embeds the executable in the mod resource directory, verifies the helper and codecs are in the resulting JAR, and publishes a test artifact (and, on main updates to code, a verified \`dist/\` package).

An ordinary source build may not include the native helper unless you have built/copied it separately. Consult [Getting Started](Getting-Started.md).

## Test / maintenance guidance

- Test normal TVs, then rectangular multi-block displays.
- Test placed and offhand Portable TV separately.
- Test one speaker, then several with distinct roles.
- Compare LOW/BALANCED/HIGH and AUTO/H264/DELTA.
- Watch for high aggregate frame/texturing cost when several displays show streams.
- Use \`/tvtime_capture status\` to capture reproducible metrics.
- Update this wiki along with changes to blocks, settings, commands, and recipe JSON.

**Source:** [GitHub Repository](https://github.com/CaszGamerMD/TVtime) · [Issues](https://github.com/CaszGamerMD/TVtime/issues)
