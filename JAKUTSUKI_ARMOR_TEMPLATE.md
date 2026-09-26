# 蛇窟姫（ジャクツキ）専用装備テンプレート

v0.30.4 では、堕狐の装備構造を基に蛇窟姫専用装備を追加しています。

## 使用素材 / CustomModelData
- 頭: IRON_HELMET / 1101 / `jakutsuki_helmet`
- 胴: IRON_CHESTPLATE / 1102 / `jakutsuki_chestplate`
- 脚: IRON_LEGGINGS / 1103 / `jakutsuki_leggings`
- 足: IRON_BOOTS / 1104 / `jakutsuki_boots`

蛇窟姫を鬼として開始すると、自動で4部位が装備されます。

## 編集するファイル
### 手持ち・インベントリアイコン
`resourcepack/assets/onigame/textures/item/`
- jakutsuki_helmet.png
- jakutsuki_chestplate.png
- jakutsuki_leggings.png
- jakutsuki_boots.png

16x16 PNG。現在は堕狐素材を黒・濃紫・毒々しい紫へ変換した仮デザインです。

### 実際に着た時の見た目
`resourcepack/assets/minecraft/textures/models/armor/`
- iron_layer_1.png
- iron_layer_2.png

Minecraft 1.20.1では CustomModelData だけで「着用中の防具モデル」を個別変更できないため、
堕狐=チェーン装備、蛇窟姫=鉄装備、と素材自体を分離しています。
これにより両者を別の着用テクスチャにできます。

`iron_layer_1.png` / `iron_layer_2.png` を64x32のまま編集すると、蛇窟姫の衣装を変更できます。

## デザイン方針（仮）
- 基調: 黒 / 濃紫
- 差し色: 毒々しい赤紫
- 和装・巫女装束寄り
- 蛇・呪術・洞窟を連想する暗い配色

現在の画像は完成版ではなく、Blockbench・画像編集で仕上げるための「動作する雛形」です。

## 注意
この方式ではリソースパック適用中の通常の鉄防具も同じ着用テクスチャになります。
鬼げぇむ側で通常の鉄防具を装備として使わない構成を前提にしています。


## v0.30.5 正式テクスチャ
ユーザー制作の蛇窟姫用防具テクスチャを正式採用しました。

- `Jatutuki_layer_1.png` → `resourcepack/assets/minecraft/textures/models/armor/iron_layer_1.png`
- `jakutuki_layer_2.png` → `resourcepack/assets/minecraft/textures/models/armor/iron_layer_2.png`

両方とも Minecraft 1.20.1 の防具レイヤー規格である 64x32 PNG です。
