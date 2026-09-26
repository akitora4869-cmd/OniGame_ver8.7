#!/usr/bin/env python3
import gzip
import math
import random
import struct
from pathlib import Path

from PIL import Image, ImageDraw

W, H, L = 120, 48, 120
AIR = "minecraft:air"
blocks = [AIR] * (W * H * L)
random.seed(260903)


def index(x, y, z):
    return (y * L + z) * W + x


def inside(x, y, z):
    return 0 <= x < W and 0 <= y < H and 0 <= z < L


def setb(x, y, z, block):
    if inside(x, y, z):
        blocks[index(x, y, z)] = block


def getb(x, y, z):
    return blocks[index(x, y, z)] if inside(x, y, z) else AIR


def fill(x1, y1, z1, x2, y2, z2, block):
    for y in range(min(y1, y2), max(y1, y2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            for x in range(min(x1, x2), max(x1, x2) + 1):
                setb(x, y, z, block)


def line_x(x1, x2, y, z, block):
    fill(x1, y, z, x2, y, z, block)


def line_z(x, y, z1, z2, block):
    fill(x, y, z1, x, y, z2, block)


def disc(cx, cy, cz, radius, block):
    for z in range(cz - radius, cz + radius + 1):
        for x in range(cx - radius, cx + radius + 1):
            if (x - cx) ** 2 + (z - cz) ** 2 <= radius ** 2:
                setb(x, cy, z, block)


def column(x, z, y1, y2, block):
    fill(x, y1, z, x, y2, z, block)


def palette_pick(options, x, z):
    return options[abs((x * 734287 + z * 912931 + x * z * 17)) % len(options)]


# Red earth foundation.
for z in range(L):
    for x in range(W):
        d = math.dist((x, z), (59.5, 59.5))
        surface = 4 + (1 if d > 48 and (x * 3 + z * 5) % 11 < 3 else 0)
        setb(x, 0, z, "minecraft:bedrock")
        for y in range(1, surface - 1):
            setb(x, y, z, palette_pick(["minecraft:deepslate", "minecraft:tuff", "minecraft:blackstone"], x + y, z))
        setb(x, surface - 1, z, palette_pick(["minecraft:red_terracotta", "minecraft:netherrack", "minecraft:coarse_dirt"], x, z))
        setb(x, surface, z, palette_pick(["minecraft:red_sand", "minecraft:red_terracotta", "minecraft:crimson_nylium", "minecraft:netherrack"], x * 2, z * 3))

# Jagged black cliff boundary.
for z in range(L):
    for x in range(W):
        edge = min(x, z, W - 1 - x, L - 1 - z)
        if edge < 7:
            height = 13 + ((x * 11 + z * 7) % 10) + (6 - edge)
            for y in range(5, min(H, height + 5)):
                if edge < 3 or random.random() > 0.16:
                    setb(x, y, z, palette_pick(["minecraft:deepslate", "minecraft:cobbled_deepslate", "minecraft:blackstone", "minecraft:basalt"], x + y, z - y))

# Central plaza and paths.
for z in range(61, 94):
    for x in range(36, 84):
        if ((x - 60) / 25) ** 2 + ((z - 77) / 18) ** 2 <= 1:
            setb(x, 4, z, palette_pick(["minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:andesite"], x, z))
for z in range(34, 67):
    width = 5 + (z % 4 == 0)
    for x in range(60 - width, 61 + width):
        setb(x, 4, z, palette_pick(["minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:cobbled_deepslate"], x, z))

# Plaza sigil and lantern centerpiece.
for r, block in [(11, "minecraft:polished_blackstone_bricks"), (7, "minecraft:cracked_stone_bricks")]:
    for deg in range(0, 360, 3):
        a = math.radians(deg)
        setb(round(60 + math.cos(a) * r), 4, round(77 + math.sin(a) * r), block)
fill(59, 5, 76, 61, 5, 78, "minecraft:polished_blackstone_bricks")
column(60, 77, 6, 8, "minecraft:dark_oak_fence")
setb(60, 9, 77, "minecraft:soul_lantern")


def stone_lantern(x, z):
    setb(x, 5, z, "minecraft:chiseled_stone_bricks")
    column(x, z, 6, 8, "minecraft:cobblestone_wall")
    fill(x - 1, 9, z - 1, x + 1, 9, z + 1, "minecraft:stone_brick_slab[type=bottom]")
    setb(x, 10, z, "minecraft:lantern")
    fill(x - 1, 11, z - 1, x + 1, 11, z + 1, "minecraft:stone_brick_slab[type=top]")


for pos in [(43, 64), (77, 64), (42, 89), (78, 89), (51, 45), (69, 45)]:
    stone_lantern(*pos)


def torii(cx, z, base_y=5, width=13, height=10, ruined=False):
    left, right = cx - width // 2, cx + width // 2
    for x in (left, right):
        fill(x - 1, base_y, z - 1, x + 1, base_y, z + 1, "minecraft:polished_blackstone_bricks")
        column(x, z, base_y + 1, base_y + height - (2 if ruined and x == right else 0), "minecraft:stripped_crimson_hyphae[axis=y]")
    line_x(left - 2, right + (0 if ruined else 2), base_y + height - 2, z, "minecraft:crimson_planks")
    if not ruined:
        line_x(left - 3, right + 3, base_y + height, z, "minecraft:polished_blackstone_brick_slab[type=top]")
        line_x(left - 1, right + 1, base_y + height - 1, z, "minecraft:dark_oak_planks")


torii(60, 55, 5, 15, 11)
torii(31, 71, 5, 13, 9, True)
torii(92, 45, 5, 11, 8, True)

# Shrine foundation and stairs.
fill(39, 4, 9, 81, 5, 36, "minecraft:polished_blackstone_bricks")
fill(42, 6, 11, 78, 7, 34, "minecraft:dark_oak_planks")
for step in range(5):
    fill(55 - step, 4 + step // 2, 36 + step, 65 + step, 4 + step // 2, 36 + step, "minecraft:stone_brick_stairs[facing=south,half=bottom,shape=straight]")

# Shrine pillars, walls, open entrance and side rooms.
for x in [43, 51, 69, 77]:
    for z in [13, 32]:
        column(x, z, 8, 18, "minecraft:stripped_dark_oak_log[axis=y]")
for x in [43, 77]:
    for z in [19, 25]:
        column(x, z, 8, 17, "minecraft:stripped_dark_oak_log[axis=y]")
fill(44, 8, 12, 76, 8, 33, "minecraft:dark_oak_planks")
fill(44, 9, 12, 50, 16, 12, "minecraft:dark_oak_planks")
fill(70, 9, 12, 76, 16, 12, "minecraft:dark_oak_planks")
fill(44, 9, 32, 76, 16, 32, "minecraft:dark_oak_planks")
fill(43, 9, 14, 43, 16, 31, "minecraft:dark_oak_planks")
fill(77, 9, 14, 77, 16, 31, "minecraft:dark_oak_planks")
for x in range(52, 69, 4):
    column(x, 13, 9, 17, "minecraft:crimson_fence")
fill(53, 9, 31, 67, 15, 31, "minecraft:red_terracotta")
fill(57, 9, 31, 63, 14, 31, "minecraft:dark_oak_planks")

# Shrine roof: broad stepped eaves with deliberate holes for decay.
roof_layers = [
    (19, 37, 83, 8, 36),
    (20, 39, 81, 9, 35),
    (21, 42, 78, 11, 33),
    (22, 45, 75, 13, 31),
]
for y, x1, x2, z1, z2 in roof_layers:
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            edge = min(x - x1, x2 - x, z - z1, z2 - z)
            if edge <= 1 or (x + z) % 5 != 0:
                setb(x, y, z, palette_pick(["minecraft:dark_oak_planks", "minecraft:dark_oak_slab[type=bottom]", "minecraft:polished_blackstone_bricks"], x + y, z))
line_z(60, 23, 12, 32, "minecraft:polished_blackstone_brick_slab[type=top]")
for x, z in [(38, 10), (40, 8), (80, 11), (82, 14), (47, 7), (74, 35)]:
    setb(x, 18 + ((x + z) % 3), z, "minecraft:dark_oak_slab[type=bottom]")

# Shrine ropes and lights.
line_x(53, 67, 16, 13, "minecraft:chain[axis=x]")
for x in [54, 57, 60, 63, 66]:
    setb(x, 15, 13, "minecraft:bone_block[axis=y]")
for x in [47, 73]:
    setb(x, 14, 14, "minecraft:soul_lantern")

# Empty skill-selection chest in front of shrine.
setb(54, 5, 51, "minecraft:chest[facing=south,type=single,waterlogged=false]")
fill(52, 4, 49, 56, 4, 53, "minecraft:polished_blackstone_bricks")


def red_tree(x, z, trunk_h, radius):
    # Dark, slightly crooked trunk.
    for y in range(5, 5 + trunk_h):
        tx = x + (1 if y > 5 + trunk_h // 2 and (x + z) % 2 == 0 else 0)
        setb(tx, y, z, "minecraft:dark_oak_log[axis=y]")
        if y < 8:
            setb(tx + 1, y, z, "minecraft:dark_oak_wood[axis=y]")
    top_y = 5 + trunk_h
    for dy in range(-2, 3):
        rr = max(1, radius - abs(dy) // 2)
        for dz in range(-rr, rr + 1):
            for dx in range(-rr, rr + 1):
                if dx * dx + dz * dz <= rr * rr + 2 and (dx * 13 + dz * 7 + dy) % 6 != 0:
                    setb(x + dx, top_y + dy, z + dz, "minecraft:nether_wart_block")
    for dx, dz in [(-radius, 0), (radius, 1), (0, -radius), (1, radius)]:
        setb(x + dx, top_y - 1, z + dz, "minecraft:shroomlight")


tree_positions = [(18, 20, 12, 5), (29, 38, 10, 4), (17, 53, 11, 5), (23, 99, 13, 5),
                  (37, 105, 9, 4), (89, 18, 13, 5), (103, 32, 10, 4), (99, 101, 12, 5),
                  (81, 104, 9, 4), (15, 78, 10, 4), (103, 67, 11, 4)]
for args in tree_positions:
    red_tree(*args)

# Smaller dead trees and rubble.
for x, z in [(31, 22), (88, 38), (26, 87), (91, 109), (12, 40), (109, 85)]:
    column(x, z, 5, 11, "minecraft:stripped_dark_oak_log[axis=y]")
    line_x(x - 3, x + 2, 10, z, "minecraft:dark_oak_log[axis=x]")
for _ in range(125):
    x, z = random.randint(10, 109), random.randint(8, 111)
    if 37 < x < 83 and 7 < z < 96:
        continue
    setb(x, 5, z, random.choice(["minecraft:cobbled_deepslate_slab[type=bottom]", "minecraft:mossy_cobblestone", "minecraft:blackstone_slab[type=bottom]"]))

# Beginner parkour course on east side.
parkour = [(88, 5, 68), (91, 6, 70), (94, 6, 72), (97, 7, 74), (100, 7, 77),
           (102, 8, 81), (100, 8, 85), (97, 9, 88), (94, 9, 91), (91, 10, 94)]
fill(85, 4, 65, 90, 4, 70, "minecraft:polished_blackstone_bricks")
for x, y, z in parkour:
    column(x, z, 5, y, "minecraft:stone_bricks")
    setb(x, y + 1, z, "minecraft:chiseled_stone_bricks")

# Fallen logs section.
for z in [79, 84, 89]:
    line_x(84, 90, 6 + (z % 2), z, "minecraft:dark_oak_log[axis=x]")
    setb(83, 5, z, "minecraft:dark_oak_wood[axis=y]")
    setb(91, 5, z, "minecraft:dark_oak_wood[axis=y]")

# Broken torii beam balance and finish platform.
column(92, 96, 5, 12, "minecraft:stripped_crimson_hyphae[axis=y]")
column(106, 96, 5, 12, "minecraft:stripped_crimson_hyphae[axis=y]")
line_x(92, 106, 12, 96, "minecraft:crimson_planks")
fill(103, 12, 99, 109, 12, 105, "minecraft:polished_blackstone_bricks")
for x in [103, 109]:
    for z in [99, 105]:
        column(x, z, 13, 15, "minecraft:cobblestone_wall")
setb(106, 13, 102, "minecraft:bell[attachment=floor,facing=north,powered=false]")

# Parkour guiding lights.
for x, z in [(86, 67), (102, 78), (92, 93), (106, 102)]:
    column(x, z, 5, 6, "minecraft:crimson_fence")
    setb(x, 7, z, "minecraft:soul_lantern")

# Sparse crimson ground plants.
for _ in range(260):
    x, z = random.randint(9, 110), random.randint(8, 111)
    if getb(x, 5, z) == AIR and getb(x, 4, z) in {"minecraft:crimson_nylium", "minecraft:netherrack", "minecraft:red_sand", "minecraft:red_terracotta"}:
        setb(x, 5, z, random.choice(["minecraft:crimson_roots", "minecraft:nether_sprouts", "minecraft:dead_bush"] ))


# Minimal NBT writer for Sponge Schematic v2.
def name_bytes(name):
    b = name.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def tag_header(tag_type, name):
    return bytes([tag_type]) + name_bytes(name)


def tag_short(name, value):
    return tag_header(2, name) + struct.pack(">h", value)


def tag_int(name, value):
    return tag_header(3, name) + struct.pack(">i", value)


def tag_byte_array(name, value):
    return tag_header(7, name) + struct.pack(">i", len(value)) + bytes(value)


def tag_int_array(name, values):
    return tag_header(11, name) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)


def tag_compound(name, children):
    return tag_header(10, name) + b"".join(children) + b"\x00"


def tag_empty_list(name, child_type=10):
    return tag_header(9, name) + bytes([child_type]) + struct.pack(">i", 0)


palette = {}
for block in blocks:
    if block not in palette:
        palette[block] = len(palette)


def varint(value):
    out = bytearray()
    while True:
        part = value & 0x7F
        value >>= 7
        if value:
            out.append(part | 0x80)
        else:
            out.append(part)
            return out


block_data = bytearray()
for block in blocks:
    block_data.extend(varint(palette[block]))

palette_tags = [tag_int(state, pid) for state, pid in palette.items()]
metadata = tag_compound("Metadata", [
    tag_compound("WorldEdit", [tag_int("Version", 0)]),
])
root_children = [
    tag_int("Version", 2),
    tag_int("DataVersion", 3465),
    metadata,
    tag_short("Width", W),
    tag_short("Height", H),
    tag_short("Length", L),
    tag_int_array("Offset", [-60, 0, -60]),
    tag_int("PaletteMax", len(palette)),
    tag_compound("Palette", palette_tags),
    tag_byte_array("BlockData", block_data),
    tag_empty_list("BlockEntities"),
    tag_empty_list("Entities"),
]
nbt = tag_compound("Schematic", root_children)

out_dir = Path(__file__).resolve().parent.parent / "OniGame-Ruined-Shrine-Lobby-v1"
out_dir.mkdir(exist_ok=True)
schem_path = out_dir / "onigame_ruined_shrine_lobby_v1.schem"
with gzip.open(schem_path, "wb", compresslevel=9) as f:
    f.write(nbt)

# Top-down planning preview.
colors = {
    "air": (25, 18, 20), "red": (104, 26, 28), "stone": (75, 72, 72),
    "wood": (58, 35, 28), "leaf": (126, 12, 24), "light": (226, 133, 48),
    "black": (29, 29, 34), "parkour": (126, 119, 105),
}
img = Image.new("RGB", (W * 6, L * 6), colors["air"])
pix = img.load()
for z in range(L):
    for x in range(W):
        top = AIR
        for y in range(H - 1, -1, -1):
            b = getb(x, y, z)
            if b != AIR and not any(p in b for p in ("roots", "sprouts", "dead_bush")):
                top = b
                break
        if any(k in top for k in ("wart_block", "crimson_nylium")): c = colors["leaf"]
        elif any(k in top for k in ("lantern", "shroomlight", "bell")): c = colors["light"]
        elif any(k in top for k in ("dark_oak", "crimson_planks", "hyphae", "fence")): c = colors["wood"]
        elif any(k in top for k in ("stone", "andesite", "cobble")): c = colors["stone"]
        elif any(k in top for k in ("deepslate", "blackstone", "basalt")): c = colors["black"]
        else: c = colors["red"]
        for dz in range(6):
            for dx in range(6):
                pix[x * 6 + dx, z * 6 + dz] = c
draw = ImageDraw.Draw(img)
draw.rectangle((60 * 6 - 8, 77 * 6 - 8, 60 * 6 + 8, 77 * 6 + 8), outline=(255, 255, 255), width=3)
img.save(out_dir / "onigame_ruined_shrine_lobby_topdown.png")

guide = """# 鬼げぇむ：朽ちた神社ロビー v1

Minecraft 1.20.1 / WorldEdit 7.2系向けのSponge `.schem`です。

## 規模

- 幅120 × 高さ48 × 奥行120ブロック
- 貼り付け基準点：中央広場の中心付近
- 空気ブロックを除外して貼り付ける前提です

## 読み込み

1. `onigame_ruined_shrine_lobby_v1.schem` をサーバーの `plugins/WorldEdit/schematics/` に入れます。
2. 貼り付けたい中央地点に立ちます。
3. 次を実行します。

```text
//schem load onigame_ruined_shrine_lobby_v1
//paste -a
```

向きが合わない場合は貼り付け前に `//rotate 90` を使ってください。貼り付け前に `//pos1` などで退避範囲を確保し、可能ならバックアップを取ってください。

## 推奨設定地点（貼り付け基準からの相対座標）

- ロビー地点：`X 0 / Y 5 / Z 18`（中央広場）
- スキル設定チェスト：`X -6 / Y 5 / Z -9`（神社前のチェスト）
- 鬼選択場所：`X 0 / Y 9 / Z -29`（神社内部）
- アスレチック開始：`X 28 / Y 5 / Z 8`
- アスレチック終了：`X 46 / Y 13 / Z 42`

実際のワールド座標は「貼り付け時に立っていた座標＋上記相対値」です。設置後、ロビー地点では `/og set lobby`、神社前のチェストを見ながら `/og skillchest` を実行してください。

## 収録エリア

- 朽ちた二層屋根の神社
- 石畳の集合広場
- 壊れた鳥居と石灯籠
- 赤い木々と赤色の大地
- 黒い岩壁の外周
- 石柱ジャンプ、倒木、鳥居梁、ゴール鐘の初心者向けアスレチック

画像と完全に同一ではなく、Minecraft上で歩きやすく編集しやすいブロック建築として再構成しています。
"""
(out_dir / "README.md").write_text(guide, encoding="utf-8")

print(f"schematic={schem_path}")
print(f"palette={len(palette)} blocks={len(blocks)} block_data={len(block_data)}")
