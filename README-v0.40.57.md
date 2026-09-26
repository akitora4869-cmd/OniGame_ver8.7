# OniGame v0.40.57 — TAG map / revival / navigation

## TAG setup
- `/og mode tag`
- `/og tag setup` — current position = TAG center + player shrine, applies 200x200 world border.
- `/og tag shrine oni` — oni release point.
- `/og tag parkour start`
- `/og tag parkour goal`
- `/og tag respawn`
- `/og tag area add <ID>` — register an area at current position.
- `/og tag area remove <ID>` / `/og tag area list`

## Player UI
TAG players no longer receive a remaining-time line in the sidebar. The sidebar shows active survivors, personal SP and area discovery state. The 60-second final-frenzy subtitle also hides the numeric remaining time in TAG.

## SP shop
Place a WHITE_SHULKER_BOX manually, look at it and run `/og tag spshop`. Other block types are rejected. Registered shops are physically hidden as AIR outside TAG mode and restored as WHITE_SHULKER_BOX in TAG mode. Product lists remain per-shop and can mix skills/items.

## Revival parkour
TAG has no normal life-stock consumption. Before the final phase, a dead player is sent to the revival parkour and is excluded from active survivors. Reaching the goal returns the player at `/og tag respawn`, restores 10 HP and grants short hit protection. Players already in the parkour when the final phase begins may finish that attempt. Deaths after the final phase are eliminations.

## Area navigation
Each player discovers areas independently. A protected COMPASS is placed in slot 9. Right-click cycles forward through discovered areas; left-click cycles backward. The compass target and a short END_ROD ground arrow point at the selected area. Navigation pauses during revival parkour and resumes after return. Discovery resets each match.

## TAG bot border
TAG player-bot flee/roam targets are clamped inside the configured 200x200 play border.
