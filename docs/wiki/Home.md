# TVtime — Community Wiki

**TVtime** is an experimental Minecraft **26.2 Fabric** mod by CaszGamerMD for broadcasting a Windows application window (including its audio) to TVs placed in a shared Minecraft world.

> **Release status:** 0.1.0-alpha.1. This wiki documents what is present in the repository's `main` branch, not a promise that every feature works perfectly. See [Known Issues](Troubleshooting.md#known-alpha-issues) before treating the mod as production-ready.

## Start here

- **[Getting Started](Getting-Started.md)** — prerequisites, installation, and first broadcast
- **[TV Screens & Channels](TV-Screens-and-Channels.md)** — wall-mounted TVs, connected displays, tuning, and display modes
- **[Broadcasting](Broadcasting.md)** — select a desktop application, start/stop streams, quality, codecs
- **[Remote & Controls](Remote-and-Controls.md)** — configure in-game devices using the remote
- **[Portable TV](Portable-TV.md)** — place a miniature TV or watch picture-in-picture from your offhand
- **[Speakers & Audio](Speakers-and-Audio.md)** — speaker designs, roles, volume, distance, and positional playback
- **[Crafting Recipes](Crafting-Recipes.md)** — confirmed recipes, plus items currently lacking recipes
- **[Commands](Commands.md)** — all current test and capture commands
- **[Troubleshooting](Troubleshooting.md)** — black screens, capture setup, sync, lag, known issues
- **[Technical Notes](Technical-Notes.md)** — architecture, limits, dev/build notes

## Quick feature overview

| Feature | Description |
| --- | --- |
| TV Screen | Thin, wall-oriented screen (3/16-block depth); adjacent matching screens form one larger display |
| Connected TV walls | Rectangular displays with matching facing, up to 16 × 16 TV blocks |
| Named channels | A broadcaster streams to a channel; multiple displays and viewers can subscribe |
| Display modes | FIT, FILL, or STRETCH |
| TV Remote | Opens the settings screen of the TV or speaker you click |
| Four speaker styles | Iron, spruce, modern, and customizable |
| Portable TV | Place it in the world or hold in offhand for a HUD picture-in-picture view |
| Windows window capture | Select an application window and broadcast its video and process audio |
| Quality profiles | LOW, BALANCED, and HIGH; AUTO/H264/DELTA codec choices |
| Positional audio | TV or speakers act as in-world sound sources |

### Scope and maturity

- The *broadcaster* uses a Windows capture helper; the native capture path is Windows-first.
- Minecraft clients use Fabric. TVtime must also be installed server-side for multiplayer media relay.
- Minecraft 26.2, Java 25+, Fabric Loader 0.19.3+, and Fabric API are required by mod metadata.
- Source files for some newer devices exist, but crafting recipes for them have **not** yet been added to `main`.
- Media/network and rendering code are experimental. Performance may vary, particularly with multiple active screens.

**Repository:** [CaszGamerMD/TVtime](https://github.com/CaszGamerMD/TVtime) · **Packaged alpha:** [dist/TVtime-0.1.0-alpha.1.jar](https://github.com/CaszGamerMD/TVtime/blob/main/dist/TVtime-0.1.0-alpha.1.jar) · **Issue tracker:** [GitHub Issues](https://github.com/CaszGamerMD/TVtime/issues)

This Markdown collection lives under `docs/wiki/` and can also be copied into GitHub's dedicated Wiki repository.
