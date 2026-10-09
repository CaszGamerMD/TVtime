# Crafting Recipes

[← Wiki home](Home.md)

**Important:** This page is sourced from the recipe JSON files currently present in the repository, not from speculative recipes or future feature ideas. In **0.1.0-alpha.1 on main**, there are only **two** recipe definitions.

## TV Screen — `tvtime:tv`

Crafting grid (top to bottom):

```text
I I I
G G R
I I I
```

| Symbol | Ingredient |
| --- | --- |
| I | Iron Ingot |
| G | Black Stained Glass Pane |
| R | Redstone Dust |

**Output:** 1 × TV Screen.

Source: [tv.json](https://github.com/CaszGamerMD/TVtime/blob/main/src/main/resources/data/tvtime/recipe/tv.json)

## Legacy Speaker — `tvtime:speaker`

```text
I I I
I N I
I R I
```

| Symbol | Ingredient |
| --- | --- |
| I | Iron Nugget |
| N | Note Block |
| R | Redstone Dust |

**Output:** 1 × Legacy Speaker.

Source: [speaker.json](https://github.com/CaszGamerMD/TVtime/blob/main/src/main/resources/data/tvtime/recipe/speaker.json)

**Note:** This recipe produces the **Legacy Speaker**, not the newer Iron/Spruce/Modern/Custom speaker blocks. The legacy speaker is hidden from the current creative tab but retained to avoid breaking earlier alpha worlds.

## Items currently lacking checked-in recipes

No crafting JSON recipes were found for the following items in `main` as of this documentation update:

- Portable TV
- TV Remote
- Iron Speaker
- Spruce Speaker
- Modern Speaker
- Custom Speaker

These devices are currently available via the **Functional Blocks** creative inventory tab. Their presence in the mod does **not** mean there is a survival crafting recipe yet. If recipes are added later, update this page from `src/main/resources/data/tvtime/recipe/`.

## Creative inventory

The Functional Blocks tab currently lists: TV Screen, Portable TV, Iron Speaker, Spruce Speaker, Modern Speaker, Custom Speaker, TV Remote.

See [TV Screens](TV-Screens-and-Channels.md), [Portable TV](Portable-TV.md), and [Speakers](Speakers-and-Audio.md).
