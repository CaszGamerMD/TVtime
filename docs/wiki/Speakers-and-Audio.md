# Speakers & Audio

[← Wiki home](Home.md)

TVtime can emit stream audio from a TV Screen or from separate placed speakers. All speakers receive media from the same **named channel** they are tuned to.

## Speaker designs

| Block | Model / use |
| --- | --- |
| **Iron Speaker** | Large metal-style speaker |
| **Spruce Speaker** | Medium wood-style speaker |
| **Modern Speaker** | Small, thin, wall-oriented speaker |
| **Custom Speaker** | Customizable cabinet/side materials |
| **Legacy Speaker** | Older full-block speaker retained for existing alpha worlds; hidden from creative tab |

The current four advertised styles are registered in the Functional Blocks creative tab. The legacy block remains registered for backward compatibility.

The **Custom Speaker** cycles its side appearance with **sneak + right-click** (without the remote): iron, spruce, black, white, copper, stone.

## Configure speakers

Right-click a speaker with the **TV Remote**. Available fields:

| Setting | Values | Default |
| --- | --- | --- |
| Channel | Same name as broadcast | Empty (untuned) |
| Speaker role | FULL, LEFT, RIGHT, CENTER, REAR_LEFT, REAR_RIGHT, LFE | FULL |
| Volume | 0.0–2.0 | 1.0 |
| Range | 1–128 blocks | 32 blocks |

The speaker's sound follows distance from its placed block.

## Audio roles

The capture path processes selected-application stereo audio, then uses positional sound emitters in Minecraft.

| Role | Current signal |
| --- | --- |
| FULL | Left + right downmix to mono |
| LEFT | Left capture channel |
| RIGHT | Right capture channel |
| CENTER | Left + right downmix to mono |
| REAR_LEFT | Left capture channel |
| REAR_RIGHT | Right capture channel |
| LFE | Left + right downmix to mono (not a true dedicated subwoofer track) |

These are **logical speaker assignments**, not discrete 7.1 surround channels from the source application.

### Multiple speakers and volume

The current playback code **normalizes duplicate speakers with the same role** for the same stream. Placing two identical-role emitters does **not** guarantee twice the volume. Speakers with different roles let you arrange positional left/right/surround-style audio.

If sound appears doubled or has timing issues, try disabling **Built-in TV audio** on the screen and using only your external speaker blocks.

## Technical and known limitations

- Windows capture supports selected-process audio and normally uses Opus (48 kHz stereo), with raw PCM fallback.
- Playback is positioned in the world, and channel roles are emitted as positional mono streams.
- The TV's own audio has a nominal 24-block range; standalone speakers have configurable range.
- Audio synchronization and performance are active alpha-testing areas. See [Troubleshooting](Troubleshooting.md).

See [Remote & Controls](Remote-and-Controls.md) or [Commands](Commands.md) for configuration steps.
