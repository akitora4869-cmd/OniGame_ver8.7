package jp.muimi.onigame;
public enum OniType {
    DAKKO("堕狐"), KISHIN("鬼王"), SHIKKI("疾鬼"), YUUKI("幽鬼"), KANKI("鬼神（オニガミ）"), JAKUTSUKI("蛇窟姫");
    public final String display;
    OniType(String display) { this.display = display; }
    public static OniType parse(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "dakko", "堕狐" -> DAKKO;
            case "kio", "kiou", "鬼王", "きおう" -> KISHIN;
            case "shikki", "疾鬼", "しっき" -> SHIKKI;
            case "yuuki", "幽鬼", "ゆうき" -> YUUKI;
            case "onigami", "kishin", "kanki", "鬼神", "オニガミ", "おにがみ", "喚鬼", "かんき" -> KANKI;
            case "jakutsuki", "蛇窟姫", "ジャクツキ" -> JAKUTSUKI;
            default -> null;
        };
    }
}
