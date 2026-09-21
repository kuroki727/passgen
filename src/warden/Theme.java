package warden;

/**
 * ANSI 256-color themes, reusing the palette names Kuroki already uses
 * across other terminal tools (tome, vitals) so the whole toolset feels
 * consistent.
 */
public enum Theme {
    EVERFOREST("Everforest", 108, 214, 167, 245, 250),
    GRUVBOX("Gruvbox", 172, 208, 167, 245, 223),
    NORD("Nord", 110, 109, 150, 245, 189),
    DRACULA("Dracula", 141, 212, 84, 245, 189),
    SOLARIZED("Solarized", 37, 136, 64, 245, 230);

    public final String label;
    private final int accent;   // headers / accents
    private final int warn;     // warnings
    private final int ok;       // success
    private final int muted;    // secondary text
    private final int text;     // primary text

    Theme(String label, int accent, int warn, int ok, int muted, int text) {
        this.label = label;
        this.accent = accent;
        this.warn = warn;
        this.ok = ok;
        this.muted = muted;
        this.text = text;
    }

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";

    private String fg(int code) {
        return "\u001B[38;5;" + code + "m";
    }

    public String accent(String s) { return fg(accent) + s + RESET; }
    public String warn(String s) { return fg(warn) + s + RESET; }
    public String ok(String s) { return fg(ok) + s + RESET; }
    public String muted(String s) { return fg(muted) + s + RESET; }
    public String text(String s) { return fg(text) + s + RESET; }
    public String bold(String s) { return BOLD + fg(text) + s + RESET; }

    public static Theme byName(String name, Theme fallback) {
        if (name == null) return fallback;
        for (Theme t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return fallback;
    }
}
