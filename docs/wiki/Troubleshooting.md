# Troubleshooting & Known Issues

[← Wiki home](Home.md)

TVtime is at version **0.1.0-alpha.1**. Treat this as a testing guide rather than a guarantee that every use case is fully reliable.

## TV displays black, offline, or no picture

1. Make sure the source is running: \`/tvtime_capture status\`.
2. Compare the broadcast channel with the TV's **exact channel name** (use the TV Remote).
3. Verify the application appears in \`/tvtime_capture windows\`.
4. Start with a **single** normal TV Screen before testing connected walls and portables.
5. Use \`FIT\`, \`BALANCED\`, and \`AUTO\` as a baseline.
6. Ensure viewers and the multiplayer server have the correct TVtime Fabric build.
7. If a placed Portable TV is blank but a normal TV works, test its copied item settings and re-tune with the remote; report the placed-TV case separately.

## Capture unavailable / no windows listed

- Desktop capture is currently **Windows-only**.
- Use the **Windows Test Bundle** if possible; it packages the native helper.
- Check \`config/tvtime/native/tvtime-capture.exe\` or supply a helper via \`TVTIME_CAPTURE_HELPER\` or \`-Dtvtime.capture.helper=<path>\`.
- Try a normal visible application window instead of exclusive fullscreen/protected-content windows.
- Close/reopen the app, use **Refresh** in the source selector, and run \`/tvtime_capture windows\` again.

## Remote or settings do not work

- Put **TV Remote** in main hand and **right-click the block** to configure it.
- The server only accepts TV/speaker settings when you are **within 8 blocks**.
- For custom speaker side styles, **sneak + right-click without the remote**.
- For PiP corner, hold **Portable TV offhand + remote main hand** and right-click away from a configurable block.
- For copying portable channel settings, use the **Portable TV in main hand** to right-click a configured TV Screen.

## No audio, doubled audio, or desync

- Confirm the TV's **Built-in TV audio** toggle and your speaker channel assignments.
- Ensure the application has actual playable audio; TVtime captures **selected-process** audio, not a universal system mix.
- Place a single speaker first and verify its channel, FULL role, range, and volume.
- Disable built-in TV audio when testing external speakers to avoid hearing several positional sound emitters at once.
- Audio/video drift during extended playback has been reported in alpha testing; if it recurs, record how long the stream has run, selected quality/codec, speaker count, and whether stopping/restarting restores sync.

## Lag spikes / low FPS / excessive CPU

- Change quality to **LOW** before starting a broadcast.
- Use **AUTO** codec to allow fallback; compare \`/tvtime_capture status\` metrics.
- Test one active screen/display first, then add additional walls and portable TVs.
- Multiple rendered TV surfaces, capture/encoding, network throughput, audio emitters, and decoding can all contribute to performance load.
- Try avoiding multiple independent capture broadcasts while troubleshooting.

## Connected screens do not join

- Every TV Screen must face the same direction.
- Build a **complete rectangular grid** with no gaps or disconnected TV blocks.
- The maximum logical display size is **16 × 16** blocks.
- Portable TVs cannot join a wall.

## Known alpha issues

The following are **previously reported problem areas**, not claims that the latest source definitely reproduces every symptom:

- Severe lag spikes when rendering multiple TVs or streaming captures.
- Audio synchronization drifting after several minutes.
- Cropping/truncation from some fullscreen source windows.
- A placed Portable TV sometimes not showing its image.
- Handheld Portable TV item appearing oversized.
- Previously encountered exception mentioning **“Origin is not a TV block”** while breaking a TV; report again if seen in a fresh build.

**For a useful bug report**, include the exact JAR/build, Minecraft 26.2, Fabric/API versions, OS, whether you are broadcasting or viewing, reproduction steps, which command was used, screenshots/logs, and \`/tvtime_capture status\` if available.

[Report an issue on GitHub](https://github.com/CaszGamerMD/TVtime/issues).
