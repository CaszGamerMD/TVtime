# Cameras & Camera Control Table (Experimental)

[← Wiki home](Home.md)

TVtime adds a **Camera** and **Camera Control Table** for security-room style video surveillance using the existing in-game TV screens.

> **Important alpha limitation:** The first implementation broadcasts a low-resolution, live **block-color perspective** from a camera position (48 × 27 pixels, at 1 frame/second). It shows nearby block changes but **does not yet render Minecraft textures, entities/mobs, particles, or shader lighting as the player's normal view does**. This is a functional first camera-feed renderer, not full GPU quality.

## Setup

1. Craft/place a **Camera** at its intended location. Its physical block position cannot be changed from the controls; to relocate it you must break, collect, and place the camera somewhere else.
2. Place a **Camera Control Table** somewhere convenient, such as a security office.
3. **Right-click** the control table to see the saved cameras in your current dimension.
4. Select a camera, enter a name and **TV channel** such as `gate_camera`.
5. Change **Camera OFF** to **Camera ON**, and press **Save**.
6. Place a **TV Screen** or connected TV wall; use the **TV Remote** to tune it to exactly `gate_camera`.
7. Watch the live block-scene feed from the camera, or tune several TVs to the same camera channel.

A camera channel works through the existing TVtime channel/session system; one live broadcaster (camera or computer capture) can occupy a named channel at once. Choose distinct names.

## Remote controls

| Control | What it changes | Allowed range |
| --- | --- | --- |
| Camera ON/OFF | Feed and active chunk ticket | Either |
| Name | Friendly name in the camera directory | Up to 32 characters |
| TV Channel | Connects the camera's feed to TV Screens/Portable TVs | Up to 64 characters |
| Pan ◀ / ▶ | Turns viewing angle left/right | −180° to +180°, 15° steps |
| Tilt ▲ / ▼ | Moves viewing angle up/down | −80° to +80°, 10° steps |
| Zoom − / + | Narrows or widens camera's view | 1× to 8×, 0.5× steps |

**Pan/tilt/zoom are optical feed settings.** The first implementation changes the transmitted perspective but does not yet physically animate the camera model.

**Saving matters:** changes in the screen are sent to the server when you click **Save**; clicking **Back** leaves without saving.

## Remote activation and chunk loading

- Active cameras receive a server-side **chunk loading + simulation ticket** while the server is running. The camera keeps its local area (radius-2 chunk ticket) loaded when no player is nearby.
- **Turning a camera OFF releases its chunk ticket** and stops its TVtime broadcast. This saves server resources.
- TVtime maintains a **saved per-dimension directory** of cameras. An offline camera is still visible from a control table and may be powered back on remotely without physically visiting it.
- On server restart, the mod attempts to restore enabled cameras from that saved directory.
- **Safety limit:** no more than **four active camera feeds per dimension** are supported by this initial implementation. Additional enabled cameras must wait until capacity becomes free.
- The camera's chunk ticket does **not** automatically send terrain/chunks to distant players for normal gameplay movement; it is meant for the camera's server-side feed/simulation.
- Only cameras in the **same dimension** as the table appear in that table's directory.

The server checks that the player is within **8 blocks of the control table** before it accepts any control changes. This is a shared console, so consider who can reach it on multiplayer servers.

## Crafting

### Camera

```text
I C I
G R G
I I I
```

**I** = Iron Ingot; **C** = Copper Ingot; **G** = Glass Pane; **R** = Redstone Dust. Output: 1 camera.

### Camera Control Table

```text
G R G
I I I
I   I
```

**G** = Glass Pane; **R** = Redstone Dust; **I** = Iron Ingot. Output: 1 control table.

Both are also in the **Functional Blocks** creative inventory tab.

## Troubleshooting

- **No cameras listed:** place and load the camera at least once so it enters the server directory; refresh the control table.
- **No TV picture:** check camera power, camera channel, TV channel, and whether that channel is occupied by another broadcaster.
- **Unloaded scene/short range:** the simple block-color ray camera cannot see beyond the chunks currently loaded on the server. It deliberately does not synchronously load a new chunk for each ray.
- **Video quality:** currently deliberately small (48 × 27 / 1 FPS); resolution and proper world rendering remain future development tasks.
- **Performance:** start with a single camera. Camera tickets and additional viewers have server/network cost.

[Report camera bugs](https://github.com/CaszGamerMD/TVtime/issues).
