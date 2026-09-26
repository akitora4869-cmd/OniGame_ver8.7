package jp.muimi.onigame;
public enum PlayerSkill {
    SPRINT("疾走", 60), INVISIBLE("隠形", 40), SMOKE("煙幕", 35), ONI_STRIKE("破鬼撃", 45), HEAL("治療", 45), OBSESSION("執念", 35), SAFE_LANDING("安定着地", 55),
    BLINK("瞬歩",60), ECHO("残響",60), CLAIRVOYANCE("霊視",45), UNYIELDING("不退転",60), SEALING_CIRCLE("封鬼陣",35), RESONANCE("共鳴",30), SUBSTITUTE("身代わり",50), DESPERATE_RUN("決死行",80), BUGEI("武芸",55);
    public final String display; public final int cooldown;
    PlayerSkill(String display, int cooldown) { this.display=display; this.cooldown=cooldown; }
    public static PlayerSkill parse(String s) {
        if (s == null) return null;
        return switch(s.toLowerCase()) { case "sprint", "疾走" -> SPRINT; case "invisible", "隠形" -> INVISIBLE; case "smoke", "煙幕" -> SMOKE; case "strike", "破鬼撃" -> ONI_STRIKE; case "heal", "治療", "治癒", "回復" -> HEAL; case "obsession", "執念" -> OBSESSION; case "landing", "safe_landing", "安定着地" -> SAFE_LANDING; case "blink", "瞬歩" -> BLINK; case "echo", "残響" -> ECHO; case "clairvoyance", "霊視" -> CLAIRVOYANCE; case "unyielding", "不退転" -> UNYIELDING; case "seal", "封鬼陣" -> SEALING_CIRCLE; case "resonance", "共鳴" -> RESONANCE; case "substitute", "身代わり" -> SUBSTITUTE; case "desperate", "決死行" -> DESPERATE_RUN; case "bugei", "武芸" -> BUGEI; default -> null; };
    }
}
