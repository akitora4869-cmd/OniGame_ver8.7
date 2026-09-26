# OniGame v0.40.59 - TAG Oni Waiting Fix

- `/og tag oni <player|me> [oni]` で指定した人間鬼を、参加人数1人のテストでも確実に鬼として扱うよう修正。
- TAG開始時、人間鬼をプレイヤー神社へ送らず鬼待機地点へ隔離。
- `/og tag oniwait` で現在位置を鬼待機地点に設定。未設定時はロビー、ロビーも未設定なら鬼出現地点を使用。
- 待機中はSPECTATORで拘束し、解放時間に `/og tag shrine oni` へ移動して鬼能力・最大狂化を適用。
