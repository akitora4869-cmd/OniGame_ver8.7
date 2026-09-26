package jp.muimi.onigame;

public enum PassiveSkill {
    LIGHT_FOOTED("軽足", "走行スタミナ消費を15%軽減"),
    DEEP_BREATH("深呼吸", "スタミナ回復量を25%増加"),
    FOCUS("集中", "スキルチェック成功範囲と回復時の移動許容を拡大"),
    ATTACK_BOOST("神喰", "鬼への与ダメージを20%増加"),
    DURABILITY_BOOST("強靭", "最大体力を20から30へ増加"),
    EXORCISM("退魔", "鬼の心臓を損壊する作業速度を15%増加"),
    COWARDICE("臆病", "スニーク中の移動速度が通常歩行より少し遅い程度まで上昇"),
    BOND("絆", "治療完了時、周囲の生存している仲間も一緒に回復"),
    NINJA_BLOOD("忍びの血統", "移動速度が常に鬼より少し遅い程度まで上昇"),
    LEAP("跳躍", "空中でスペースをもう一度押すと視線方向へ大きく跳躍"),
    AFTERMIND("残心", "GREAT後、次のスキルチェック位置を事前に把握しやすくなる"),
    PARRY("受け流し", "鬼の攻撃直前にSHIFTでダメージ軽減と横回避"),
    QUICK_TURN("急転", "走行方向に対して横を向いてSHIFTするとサイドステップ"),
    SILENT_BREATH("息殺し", "静止スニークを続けると鬼の音響感知から消える"),
    LAST_RESERVE("土壇場", "スタミナ切れ直前に走行を止めるとスタミナを即時回復"),
    PRACTICED("手馴れ", "心臓損壊を続けるほど作業速度上昇。失敗・中断でリセット"),
    DECOY("陽動", "スニーク＋右クリックで偽の逃走者を投擲。12秒間鬼の音響感知を欺き、鬼が囮へ接近すると5秒間加速"),
    CORNERED_RAT("窮鼠", "鬼の攻撃直前に進行方向を反転していると短時間加速"),
    DIVINE_TECHNIQUE("神技", "移動速度・対鬼攻撃力・防御力を常時わずかに上昇"),
    DOUBLE_STAKES("賭け金二倍", "スキルチェックはGREATのみ成功。連続GREATで心臓損傷ボーナスが増加"),
    THREE_PHASE("三相", "走・破・援の3種トークンを揃えると次のメインスキルCTを半減"),
    FOOTSTEP_THIEF("足跡泥棒", "鬼の近くで見つからずに潜むと隠密トークン。4個で次の発見時に追跡情報を短時間遮断"),
    DEADLINE_ACCOUNTING("死線勘定", "鬼の至近距離で攻撃を受けずに危機を切り抜けると死線トークン。3個で次のメインスキルCTを短縮");

    public final String display;
    public final String description;

    PassiveSkill(String display,String description){this.display=display;this.description=description;}
}
