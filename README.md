# TVtime

**TVtime** is a Minecraft **26.2 Fabric** mod that lets players broadcast a selected **Windows application window and its audio** to shared, in-world TV screens.

**Current version:** 0.1.0-alpha.1 — experimental development build.

## Features

- Thin, wall-oriented **TV Screen** blocks that form connected rectangular displays (up to 16 × 16).
- Named broadcast channels, **FIT / FILL / STRETCH** video layouts, and optional built-in TV audio.
- **TV Remote** configuration screens for displays and speakers.
- Four speaker styles: **Iron, Spruce, Modern, Custom**, plus the legacy speaker kept for older worlds. Supports positional audio and speaker roles.
- **Experimental security cameras**, controlled through a Camera Control Table (pan/tilt/zoom, power on/off, named TV channels, local chunk tickets). The initial feed is a low-resolution block-color view, not full Minecraft rendering.
- **Portable TV** as a placed display or offhand picture-in-picture overlay with four corner choices.
- Windows application-window capture, selectable quality and codecs, and multiplayer relay.

## Documentation

📺 **[Read the TVtime Wiki / user guide](docs/wiki/Home.md)**

Start with [installation and your first broadcast](docs/wiki/Getting-Started.md), explore the [block/item guide](docs/wiki/Home.md), review [crafting recipes](docs/wiki/Crafting-Recipes.md) and [commands](docs/wiki/Commands.md), or use [troubleshooting](docs/wiki/Troubleshooting.md).

## Requirements

Minecraft Java **26.2**, Fabric Loader **0.19.3+**, Fabric API, and **Java 25+**. Windows is required for the current native *broadcasting* helper; the project is Windows-first and viewer-platform compatibility may vary. Install TVtime on clients and on the Fabric multiplayer server.

## Alpha build

A development test JAR and notes are checked in under [dist/](dist/). For a capture-ready Windows bundle, use the repository's **Windows Test Bundle** GitHub Actions artifact; ordinary builds may lack the bundled native helper. See the [installation guide](docs/wiki/Getting-Started.md).

## Issues and contributions

Please report bugs with build details and reproduction steps in [GitHub Issues](https://github.com/CaszGamerMD/TVtime/issues). TVtime is under active development; GUI, capture, audio synchronization, and multi-display performance may still need testing.

MIT licensed — see [LICENSE](LICENSE).
