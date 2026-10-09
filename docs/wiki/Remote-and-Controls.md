# Remote & Controls

[← Wiki home](Home.md)

The **TV Remote** is the main in-game control for TVtime devices. It is in the **Functional Blocks** creative inventory tab and has a maximum item stack size of one.

## Configure a TV

1. Hold the remote in your **main hand**.
2. **Right-click** a placed TV Screen or Portable TV.
3. In **TVtime TV Settings**, set:
   - **Channel:** the name of the broadcast to watch (maximum 64 characters in the GUI).
   - **Display mode:** FIT, FILL, or STRETCH.
   - **Built-in TV audio:** on/off.
   - **Broadcast source…:** open window selection, quality, codec, and broadcast controls.
4. Press **Done** to save, or **Cancel** to leave without submitting changes.

On a connected TV wall, settings apply to the entire detected rectangle. On a placed Portable TV, they apply only to that portable display.

## Configure a speaker

1. Aim at any TVtime speaker block and right-click with the remote.
2. Choose the channel, output role, volume, and hearing range.
3. Press **Done**.

See [Speakers & Audio](Speakers-and-Audio.md) for precise ranges and roles.

## Change custom speaker housing

**Sneak + right-click** a **Custom Speaker** to cycle its visible side material. Do this **without holding the TV Remote**. The current sequence is:

\`IRON → SPRUCE → BLACK → WHITE → COPPER → STONE → IRON\`

## Configure Portable TV picture-in-picture

Place a **Portable TV in your offhand** and hold the **TV Remote in your main hand**, then right-click while not targeting a configurable block. This opens **Portable TV PiP Settings**, where you can pick any screen corner.

The PiP preference is stored on the portable item. See [Portable TV](Portable-TV.md).

## Command alternative

You can also look directly at a placed block and run the \`/tvtime_capture tune\` or \`/tvtime_capture speaker\` commands. See [Commands](Commands.md).

**Distance rule:** The multiplayer server accepts TV/speaker configuration messages only from players within 8 blocks of the targeted block (squared-distance check ≤ 64).
