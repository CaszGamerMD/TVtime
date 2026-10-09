# Commands

[← Wiki home](Home.md)

The commands below are **client-side Caszual TV Time commands**, useful both for setup and diagnostics. Configuration commands operate on the **block you are looking at** and still require the connected server to accept Caszual TV Time packets.

## Real application capture: `/caszual_tv_time_capture`

| Command | Purpose |
| --- | --- |
| `/caszual_tv_time_capture windows` | List capturable Windows application windows and indices (up to 20 printed) |
| `/caszual_tv_time_capture start <index> <channel>` | Broadcast the listed window on the named channel |
| `/caszual_tv_time_capture stop` | Stop capture |
| `/caszual_tv_time_capture quality <LOW\|BALANCED\|HIGH>` | Choose capture size and FPS (while stopped) |
| `/caszual_tv_time_capture codec <AUTO\|H264\|DELTA>` | Choose video codec (while stopped) |
| `/caszual_tv_time_capture status` | Show FPS, kbps, codecs, preset, compression ratio |
| `/caszual_tv_time_capture tune <channel> [FIT\|FILL\|STRETCH] [true\|false]` | Tune the TV you are looking at, optionally choosing mode and built-in audio |
| `/caszual_tv_time_capture speaker <channel> <role> [volume] [range]` | Configure the speaker you are looking at |

`tune` defaults to FIT and built-in audio enabled. `speaker` defaults to volume 1.0 and range 32.

### Examples

```mcfunction
/caszual_tv_time_capture windows
/caszual_tv_time_capture quality LOW
/caszual_tv_time_capture codec AUTO
/caszual_tv_time_capture start 2 cinema
/caszual_tv_time_capture status
/caszual_tv_time_capture tune cinema FIT true
/caszual_tv_time_capture speaker cinema LEFT 1.0 32
/caszual_tv_time_capture speaker cinema RIGHT 1.0 32
/caszual_tv_time_capture stop
```

## Synthetic broadcast testing: `/caszual_tv_time_test`

This alternate command family lets developers test network/media presentation without starting a real application-window capture.

| Command | Purpose |
| --- | --- |
| `/caszual_tv_time_test start <channel>` | Start the test-pattern broadcaster on a channel |
| `/caszual_tv_time_test stop` | Stop test broadcast |
| `/caszual_tv_time_test tune <channel> [mode] [audio]` | Tune targeted TV |
| `/caszual_tv_time_test speaker <channel> <role> [volume] [range]` | Configure targeted speaker |

Use roles: `FULL`, `LEFT`, `RIGHT`, `CENTER`, `REAR_LEFT`, `REAR_RIGHT`, or `LFE`.

## Common command mistakes

- Run `windows` before assuming an index. Window indices are local and may change.
- A TV must be **looked at directly** before using `tune`.
- Speakers must be **looked at directly** before using `speaker`.
- The broadcaster and tuned devices must use the **same channel name**.
- Volume must be 0.0–2.0; range must be 1–128.
- If the server does not have Caszual TV Time installed, TV/speaker configuration packets cannot be accepted.
- A player must be within 8 blocks of the configured block for the server to apply settings.

See [Broadcasting](Broadcasting.md) and [Troubleshooting](Troubleshooting.md).
