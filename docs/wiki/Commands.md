# Commands

[← Wiki home](Home.md)

The commands below are **client-side TVtime commands**, useful both for setup and diagnostics. Configuration commands operate on the **block you are looking at** and still require the connected server to accept TVtime packets.

## Real application capture: `/tvtime_capture`

| Command | Purpose |
| --- | --- |
| `/tvtime_capture windows` | List capturable Windows application windows and indices (up to 20 printed) |
| `/tvtime_capture start <index> <channel>` | Broadcast the listed window on the named channel |
| `/tvtime_capture stop` | Stop capture |
| `/tvtime_capture quality <LOW\|BALANCED\|HIGH>` | Choose capture size and FPS (while stopped) |
| `/tvtime_capture codec <AUTO\|H264\|DELTA>` | Choose video codec (while stopped) |
| `/tvtime_capture status` | Show FPS, kbps, codecs, preset, compression ratio |
| `/tvtime_capture tune <channel> [FIT\|FILL\|STRETCH] [true\|false]` | Tune the TV you are looking at, optionally choosing mode and built-in audio |
| `/tvtime_capture speaker <channel> <role> [volume] [range]` | Configure the speaker you are looking at |

`tune` defaults to FIT and built-in audio enabled. `speaker` defaults to volume 1.0 and range 32.

### Examples

```mcfunction
/tvtime_capture windows
/tvtime_capture quality LOW
/tvtime_capture codec AUTO
/tvtime_capture start 2 cinema
/tvtime_capture status
/tvtime_capture tune cinema FIT true
/tvtime_capture speaker cinema LEFT 1.0 32
/tvtime_capture speaker cinema RIGHT 1.0 32
/tvtime_capture stop
```

## Synthetic broadcast testing: `/tvtime_test`

This alternate command family lets developers test network/media presentation without starting a real application-window capture.

| Command | Purpose |
| --- | --- |
| `/tvtime_test start <channel>` | Start the test-pattern broadcaster on a channel |
| `/tvtime_test stop` | Stop test broadcast |
| `/tvtime_test tune <channel> [mode] [audio]` | Tune targeted TV |
| `/tvtime_test speaker <channel> <role> [volume] [range]` | Configure targeted speaker |

Use roles: `FULL`, `LEFT`, `RIGHT`, `CENTER`, `REAR_LEFT`, `REAR_RIGHT`, or `LFE`.

## Common command mistakes

- Run `windows` before assuming an index. Window indices are local and may change.
- A TV must be **looked at directly** before using `tune`.
- Speakers must be **looked at directly** before using `speaker`.
- The broadcaster and tuned devices must use the **same channel name**.
- Volume must be 0.0–2.0; range must be 1–128.
- If the server does not have TVtime installed, TV/speaker configuration packets cannot be accepted.
- A player must be within 8 blocks of the configured block for the server to apply settings.

See [Broadcasting](Broadcasting.md) and [Troubleshooting](Troubleshooting.md).
