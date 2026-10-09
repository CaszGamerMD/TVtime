# Crafting Recipes

[← Wiki home](Home.md)

**Important:** This page is sourced from the recipe JSON files currently present in the repository, not from speculative recipes or future feature ideas. In **0.1.0-alpha.2 on main**, the base mod had **two** recipes; the CCTV feature branch adds **two more** (camera and control table).

## TV Screen — `caszual_tv_time:tv`

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

Source: [tv.json](__CASZUAL_TV_TIME_REPOSITORY_URL__/blob/main/src/main/resources/data/caszual_tv_time/recipe/tv.json)

## Legacy Speaker — `caszual_tv_time:speaker`

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

Source: [speaker.json](__CASZUAL_TV_TIME_REPOSITORY_URL__/blob/main/src/main/resources/data/caszual_tv_time/recipe/speaker.json)

**Note:** This recipe produces the **Legacy Speaker**, not the newer Iron/Spruce/Modern/Custom speaker blocks. The legacy speaker is hidden from the current creative tab but retained to avoid breaking earlier alpha worlds.

## Camera & Camera Control Table

See the [Camera and Control Table guide](Cameras-and-Control-Table.md#crafting) for both new 3 × 3 shaped recipes.

The **Camera** uses iron ingots, copper ingot, glass panes, and redstone; the **Camera Control Table** uses iron ingots, glass panes, and redstone.

## Items currently lacking checked-in recipes

No crafting JSON recipes were found for the following items in `main` as of this documentation update:

- Portable TV
- TV Remote
- Iron Speaker
- Spruce Speaker
- Modern Speaker
- Custom Speaker

These devices are currently available via the **Functional Blocks** creative inventory tab. Their presence in the mod does **not** mean there is a survival crafting recipe yet. If recipes are added later, update this page from `src/main/resources/data/caszual_tv_time/recipe/`.

## Creative inventory

The Functional Blocks tab currently lists: TV Screen, Camera, Camera Control Table, Portable TV, Iron Speaker, Spruce Speaker, Modern Speaker, Custom Speaker, TV Remote.

See [TV Screens](TV-Screens-and-Channels.md), [Portable TV](Portable-TV.md), and [Speakers](Speakers-and-Audio.md).
