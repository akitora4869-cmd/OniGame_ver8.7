package jp.muimi.onigame;

public enum OniPassiveSkill {
    CRAVING("渇望", "追跡が続くほど段階的に移動速度が上昇"),
    BLOOD_SCENT("血嗅", "負傷したぷれいやーを一定間隔で短時間発光"),
    GUARDIAN("守護", "心臓が損壊された際の通知間隔を短縮"),
    PREDATION("捕食", "ぷれいやーを瀕死・脱落させるとスタミナ回復"),
    INTERFERENCE("妨害", "心臓損壊の成功判定を狭め、通常成功時に進行を後退"),
    MASTERY("熟達", "鬼スキルのクールタイムを短縮"),
    MOMENTUM("余勢", "鬼スキル発動時に少量のスタミナを回復"),
    ANCHOR("不動", "ノックバック耐性を上げ、正面戦闘や溜め技を安定化"),
    PULSE("脈動", "心臓損壊の警報を受けると短時間加速"),
    EXECUTION("狩印", "発光中の獲物へ与えるダメージが増加"),
    HUNT_RECORD("狩猟記録", "異なるぷれいやーを発見して記録。複数人を狩るほど索敵報酬を得る"),
    GRUDGE_RETURN("怨返し", "同じぷれいやーから妨害を受けると怨トークンを蓄積し、3個で次の妨害への耐性を得る"),
    BLOOD_MARK("血印", "攻撃したぷれいやーへ血印を刻む。3印の獲物を再攻撃すると回収して鬼スキルCTを短縮"),
    SCENT_TRAIL("残り香", "追跡を振り切られた地点へ短時間、獲物の残留痕跡を表示"),
    HUNTERS_INSTINCT("狩人の勘", "一定時間獲物を発見できないと最寄りのぷれいやーの方向を感知"),
    FOOTSTEP_HUNTER("足音狩り", "走っているぷれいやーへの発見距離が拡大"),
    OBSESSION_HUNT("執着", "追跡を振り切られた獲物を再発見すると短時間加速"),
    HEART_EYE("心眼", "心臓が攻撃されている間、その心臓の位置を強く可視化"),
    CURSED_VEIN("呪脈", "心臓破壊時、その周囲のぷれいやーへ短時間の弱体化を付与"),
    BACKFLOW("逆流", "心臓破壊時、その周囲のぷれいやーを鬼の瘴気で吹き飛ばす"),
    LAST_FORTRESS("最後の砦", "残り心臓が少ないほど鬼が耐性と機動力を得る"),
    BLOOD_FRENZY("血狂い", "短時間に連続命中するほど与えるダメージが段階的に上昇"),
    PRESSURE("重圧", "鬼の至近距離にいるぷれいやーへ継続的に鈍足を与える"),
    HUNTING_GROUND("狩場", "同じ場所で戦い続けると短時間、鬼の攻撃性能が上昇"),
    FINISHER_CHASE("追い打ち", "低HPのぷれいやーへ命中すると短時間加速"),
    SEE_THROUGH("看破", "隠形・残響などで気配を消したぷれいやーの大まかな方向を感知"),
    SPELL_BREAK("破術", "妨害を受けるたび耐性を蓄積し、次の妨害時間を軽減"),
    SKILL_SEAL("封殺", "鬼の攻撃を受けたぷれいやーのスキルを短時間封印"),
    ADAPTATION("適応", "同じぷれいやーから妨害を受け続けるほど妨害時間をさらに短縮"),
    BEAST_PATH("獣道", "追跡開始直後に短時間の移動速度上昇"),
    AMBUSH("急襲", "長時間未追跡の状態から獲物を発見すると強い初動加速"),
    TERRITORY("縄張り", "心臓付近では鬼スキルのクールタイムが短縮"),
    HOMING("帰巣", "遠くの心臓が攻撃されると短時間、帰還用の加速を得る");

    public final String display;
    public final String description;

    OniPassiveSkill(String display,String description){this.display=display;this.description=description;}
}
