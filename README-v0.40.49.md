# OniGame v0.40.49

- ぷれいやーBotは従来どおり2つのパッシブを所持。
- `player-bot.extra.enabled: true` のEXTRAでは、メインスキルごとに相性の良い2パッシブへ自動最適化。
- DUO相方鬼Botにも2つの鬼パッシブ構成を追加。
- DUO EXTRAは鬼種別シナジー構成、ELITE/NORMALも有効な構成を選択。
- DUO相方の熟達は実際のシグネチャースキルCTへ反映。不動はKB耐性へ反映。余勢はスキル後の短時間加速へ反映。

EXTRA例:
- 堕狐: 熟達 + 獣道
- 鬼王: 不動 + 血狂い
- 疾鬼: 獣道 + 追い打ち
- 幽鬼: 急襲 + 看破
- 蛇窟姫: 熟達 + 重圧

※ この作業環境には Maven がないため、GitHub Actions で `mvn clean package` の確認が必要です。
