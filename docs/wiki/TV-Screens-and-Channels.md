# TV Screens & Channels

[← Wiki home](Home.md)

## TV Screen

The TV Screen is a directional, wall-oriented display with a collision box **3/16 of a block deep**. Its facing is determined by placement. Use the [TV Remote](Remote-and-Controls.md) to configure the channel, video layout, and built-in audio.

## Connected displays

Adjacent **TV Screen** blocks with the **same facing** are scanned into a rectangular display. The rectangle can be as large as **16 wide × 16 high** (256 TV Screen blocks). Build an unbroken rectangular wall rather than a shape with gaps or staggered edges.

For example, a 4 × 3 rectangle of aligned TV Screens is treated as one 4 × 3 display. The same video is laid out across that display rather than independently repeated on every block.

**Important:** Portable TVs are separate 1-block displays and do not combine into a TV wall. Screens facing different directions do not join.

A single TV configuration applies its channel, display mode, and audio setting to the detected connected rectangle.

## Channels

A channel is a name that links one broadcaster to any number of tuned TV displays and speakers. Examples: `movies`, `music`, `game`.

- Select a channel when starting your broadcast.
- Give a TV exactly the same channel name using the remote or `/tvtime_capture tune`.
- Link a speaker by assigning that same name.
- More than one screen can show the same channel.
- Only one active broadcaster can occupy a given channel at a time.
- The server currently permits **one outbound broadcast per player**.

If the channel is empty, offline, or different from the broadcaster's channel, the TV will not show a live broadcast.

## Picture layout modes

| Mode | Result |
| --- | --- |
| `FIT` | Preserve aspect ratio and show the entire picture; may leave empty bars |
| `FILL` | Preserve aspect ratio and fill the screen; may crop outer edges |
| `STRETCH` | Fill the display, even if it distorts the aspect ratio |

**Recommendation:** start with FIT, then try FILL for an edge-to-edge screen.

## TV audio

TV Screen settings include **Built-in TV audio**. When enabled, the connected display emits positional audio near the center of the screen. The current client scanner uses a **24-block nominal TV audio range**.

If you are using external [speakers](Speakers-and-Audio.md), you can disable built-in TV audio to avoid multiple audio emitters.

## Setting up multiple screens

Build the displays wherever you want, then tune each to the desired channel. A kitchen TV, theater TV, and Portable TV can all display the same stream, subject to network and rendering performance.

See [Remote & Controls](Remote-and-Controls.md) and [Commands](Commands.md) for setup.
