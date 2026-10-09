# Getting Started

[← Wiki home](Home.md)

## Requirements

| Component | Requirement |
| --- | --- |
| Game | Minecraft Java Edition 26.2 |
| Loader | Fabric Loader 0.19.3 or newer |
| API | Matching Fabric API |
| Java | Java 25 or newer |
| Broadcaster | Windows, for the native application-window capture helper |
| Viewer | Minecraft client with the Caszual TV Time mod; non-Windows viewing is not established by current testing |
| Multiplayer server | Caszual TV Time + Fabric API installed for channel management and media relay |

These are the values in `fabric.mod.json` and `gradle.properties`. Caszual TV Time is **alpha** software.

## Installation

1. Install the appropriate Minecraft 26.2 Fabric profile and Fabric API.
2. Obtain a compatible Caszual TV Time JAR. The repository contains an [alpha JAR in dist/](https://github.com/CaszGamerMD/TVtime/tree/main/dist). For a verified Windows capture-helper package, check the **Windows Test Bundle** GitHub Actions artifact.
3. Put Caszual TV Time in your Minecraft instance's `mods` folder; install it on your Fabric server too if you are playing multiplayer.
4. Restart Minecraft (and the server, if applicable).
5. Find **TV Screen**, **Portable TV**, **Iron Speaker**, **Spruce Speaker**, **Modern Speaker**, **Custom Speaker**, and **TV Remote** in the **Functional Blocks** creative inventory tab.

### Windows capture helper

The Windows Test Bundle packages a native helper in the JAR. When present, the mod automatically extracts `caszual-tv-time-capture.exe` to:

`<Minecraft game directory>/config/caszual_tv_time/native/caszual-tv-time-capture.exe`

If a build lacks the helper, place `caszual-tv-time-capture.exe` in the game directory or `config/caszual_tv_time/`, or specify the `CASZUAL_TV_TIME_CAPTURE_HELPER` environment variable or the JVM property `-Dcaszual_tv_time.capture.helper=<path>`.

The generic Linux GitHub Actions build may produce a mod JAR **without** the native Windows capture helper. Prefer the Windows bundle when you plan to broadcast.

## Your first broadcast (GUI)

1. Place a **TV Screen** on a wall.
2. Hold the **TV Remote** and right-click the TV Screen.
3. In **Caszual TV Time TV Settings**, choose a channel name, such as `movies`. Set **Display mode** to **FIT** initially, and leave **Built-in TV audio** on.
4. Choose **Broadcast source…**. The screen lists available capturable Windows application windows.
5. Select **BALANCED** quality and **AUTO** codec to start; click the window you want to broadcast. This starts a broadcast request on the chosen channel.
6. Back on the TV settings screen, save with **Done** so the TV is tuned to the same channel.
7. Watch the TV. Other viewers with the mod can see the channel on TVs tuned to it.

Only stream windows and audio you are entitled to share. Capture compatibility can vary by app or Windows configuration.

## Your first broadcast (commands)

On the **Windows broadcasting client**, run:

```mcfunction
/caszual_tv_time_capture windows
/caszual_tv_time_capture start 0 movies
```

Here `0` is only an example window index; pick the index from your own list. Look at a TV Screen and run:

```mcfunction
/caszual_tv_time_capture tune movies
```

Stop afterward with `/caszual_tv_time_capture stop`. The `/caszual_tv_time_capture` commands are **client-side**; the server must support Caszual TV Time's networking.

## Next

See [TV Screens & Channels](TV-Screens-and-Channels.md), [Broadcasting](Broadcasting.md), [Remote & Controls](Remote-and-Controls.md), and [Troubleshooting](Troubleshooting.md).
