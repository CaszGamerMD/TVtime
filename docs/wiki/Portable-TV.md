# Portable TV

[← Wiki home](Home.md)

The Portable TV is a smaller screen you can either **place in the world** or **hold in your offhand** for a small picture-in-picture view over the Minecraft HUD.

## Copy settings from a TV

1. Configure a regular TV Screen with the remote, including its channel.
2. Hold a **Portable TV item in your main hand**.
3. **Right-click the configured regular TV Screen** with that item.

This copies the channel, display mode, and audio enabled/disabled state from the TV to the portable item's saved settings. Your selected picture-in-picture corner is preserved.

> This interaction uses the Portable TV item, **not** the remote.

## Picture-in-picture (PiP)

1. Tune the Portable TV item using the copying steps above.
2. Put the Portable TV in your **offhand**.
3. If the assigned channel is live, video appears as a small framed overlay on the HUD. The source is scaled to fit within approximately **160 × 90 HUD pixels**.
4. If the channel exists on the item but the broadcast is offline, an **OFFLINE** frame is shown. An untuned item with an empty channel does not show a frame.

The PiP can optionally play audio near your player (the implementation uses a lower volume than a normal TV).

### Choose the corner

Hold the **TV Remote in main hand**, keep the **Portable TV in offhand**, then right-click to open **Portable TV PiP Settings**. Pick:

- Top left
- Top right
- Bottom left
- Bottom right (**default**)

The overlay reserves extra space along the lower HUD so it does not sit directly on the bottom-center HUD.

## Place the Portable TV

You can place it as its own decorative, functional TV block. A placed Portable TV uses the item's copied channel, mode, and audio setting. Use the **TV Remote** to adjust it afterward. Portable TVs do **not** connect into a multi-block wall.

## Limitations

Portable video rendering is still part of the alpha test surface. If the placed TV shows no picture, check the live channel and saved settings, test a normal TV with the same channel, and include details in an issue. See [Troubleshooting](Troubleshooting.md).

The handheld item model and multi-display rendering performance have also been reported as areas needing improvement; a UI setting existing in the source does not imply all rendering issues are resolved.
