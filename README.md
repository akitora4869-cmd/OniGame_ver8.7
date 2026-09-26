# v0.39.6 切断保護 Phase 1

ベース: v0.39.4 Duo OniBot補充修正版

## 今回の実装
- 試合中の鬼/ぷれいやー切断を即脱落・即勝敗にしない。
- 既定60秒の再接続猶予を追加 (`disconnect.grace-seconds`)。
- 猶予中は役職スロットを保持し、ゲームタイマーは継続。
- 60秒以内に戻れば同じ参加者/役職として復帰。
- 鬼は復帰時に `oniTeam` を再確認し、鬼枠を維持。
- ぷれいやーは復帰時に即死/脱落扱いにならない。
- 猶予切れ時のみ、ぷれいやーは脱落扱い、鬼は残存鬼/鬼Botを確認して勝敗判定。
- 切断時に追跡BGM/追跡状態を解除して残留音を防止。
- ゲーム終了時に切断猶予状態を全消去。

## 段階実装について
これは安全性を優先した Phase 1 です。
次段階では「切断者の代行Bot生成」「復帰時に代行Botの位置/HP/状態を本人へ返す」を追加できます。
既存Botが単体状態を多く共有しているため、Phase 1と分離して実装する方がDuo/鬼神召喚Botとの混同を避けられます。


## v0.39.9 Disconnect Proxy Phase 2B
- 鬼プレイヤー切断時にも60秒の再接続猶予を維持しつつ、切断地点へ「鬼代行Bot」を生成します。
- 鬼代行Botは切断した鬼の位置・HP・装備を引き継ぎ、ぷれいやー陣営を追跡します。
- 再接続時は鬼代行Botの現在位置・HPを本人へ戻して代行Botを削除します。
- 60秒を超えた場合は、生存中の鬼代行Botがその鬼枠を継続します。
- Duoでは片方の鬼が切断しても、もう片方の人間鬼／通常鬼Bot／鬼代行Botを含めて勝敗判定します。
- 鬼代行Botは `isOni` に含め、鬼同士のフレンドリーファイア除外対象です。
- Phase 2Bでは安全性優先で、鬼代行Botは基本追跡・近接戦闘までです。固有スキルAIの完全継承は次段階です。


## v0.39.9 Disconnect Proxy Phase 2C
- 鬼代行Botが切断時の鬼タイプを保持します。
- 堕狐 / 鬼王 / 疾鬼 / 幽鬼 / 鬼神（オニガミ） / 蛇窟姫ごとの簡易固有AIを追加しました。
- 代行BotごとにスキルCTと行動サイクルを独立管理し、Duoの片方が切断しても通常鬼BotのグローバルCTを奪いません。
- 再接続時は従来どおり代行Botの現在位置・HPを本人へ返します。

## v0.40.1 鬼域 Phase 2A/2B
- 第二覚醒（必要心臓50%）で鬼域展開アイテムを解放。
- 共通: 半径28 / 60秒 / CT90秒（config.ymlで調整）。
- 疾鬼「狩場」: 領域内でダブルジャンプ、着地まで最大3回の壁蹴り。跳躍狩りと連携可能。
- 堕狐「狐境」: SHIFT+右クリックで分霊を最大3体配置、通常右クリックで視線方向の分霊と「狐換え」。
- Phase 2A/2Bの試作基盤。鬼核・領域解除・偽狐火・複合鬼域は後続Phaseで追加予定。

## v0.40.4 Area Action Points
巨大マップ向けの移動ギミック基盤を追加。

- `UPDRAFT` 上昇気流: 下から上へ流れる粒子柱。高所へ打ち上げ、落下ダメージを一時保護。
- `JUMP_PAD` 跳躍床: 設置時の向きへ大ジャンプ。
- `SPIRIT_ROAD` 霊道: 設置時の向きへ高速移動する一方向ルート。
- `SAFE_DROP` 安全落下: 下層へ素早く降り、着地ダメージを保護。
- 手動: `/og area add <updraft|jump_pad|spirit_road|safe_drop> <id> [値]`
- 管理: `/og area list`, `/og area show`, `/og area remove <id>`
- 自動候補: `/og area autogen [半径]` → `/og area accept <番号>` / `/og area acceptall`
- 自動候補生成はブロックを変更せず、高低差の大きい地点を解析して候補だけ表示する。

## v0.40.28 - Azakuji Elite NPC
- 字九字ひろをプレイヤー選択不可のNPC友軍専用キャラクターへ変更。
- 構成: 武芸 / 神喰 / 神技・極 / 守護 / 神性憑依。
- 神技・極による高機動ヒット&アウェイ、戦闘跳躍、救援優先AIを強化。
- 神性憑依: HP30%以下で1試合1回、22秒間の超強化。
- 蛇窟姫以外でも通常戦へ確率参戦可能。
- `/og azakuji on|off|status|chance <0-100>` で通常戦参戦と確率を設定可能。

## v0.40.37 Equipment Registry / Diamond Oni Armor
- Added `equipment.yml` as the first ID-based equipment registry.
- Every Oni armor piece now uses a `DIAMOND_*` base item.
- Every Oni + armor slot has a stable PDC ID (`onigame:equipment_id`) and unique CustomModelData in 3101-3604.
- Human Oni, main OniBot, and DUO OniBot use the same registry.
- Startup checks duplicate CustomModelData and logs a warning.
- Reserved convention: 2000-2999 player equipment, 3000-3999 Oni equipment, 4000-4999 NPC equipment, 10000+ UI/skill icons.

Important: Minecraft Java 1.20.1 vanilla resource packs can select the *inventory/held item model* by CustomModelData, but worn armor textures do not natively switch by CustomModelData. The IDs/CMDs are now stable and ready for resource-pack mapping; custom worn armor appearance still needs the project's existing armor-rendering technique or an OptiFine/CIT/CEM-compatible approach.

## v0.40.63 Terra objective
TAG `Terra` now supports four individually collected objective gems (diamond/gold/emerald/lapis), color-matched VFX, and a personal 4-stage talisman charge.
# OniGame v0.40.77 - Terra Gem Visibility

- Terra's registered diamond/gold/emerald/lapis objective blocks are now physically hidden as AIR whenever the selected game mode is not TAG.
- Switching to TAG restores each registered block with its proper material.
- Switching away from TAG hides them again without deleting their registered locations.
- Plugin startup also synchronizes gem visibility with the currently selected mode.
- Registering/re-registering a Terra gem immediately synchronizes visibility.
