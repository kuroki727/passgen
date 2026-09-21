package warden;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Non-sensitive preferences, stored in plain text — never touches passwords. */
public final class Settings {
    public int defaultLength = 16;
    public boolean upper = true;
    public boolean lower = true;
    public boolean digits = true;
    public boolean symbols = true;
    public boolean excludeAmbiguous = false;
    public Theme theme = Theme.EVERFOREST;

    private final Path file;

    public Settings(Path file) {
        this.file = file;
    }

    public static Settings load(Path file) {
        Settings s = new Settings(file);
        if (!Files.exists(file)) return s;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            return s;
        }
        s.defaultLength = parseInt(p.getProperty("length"), s.defaultLength);
        s.upper = parseBool(p.getProperty("upper"), s.upper);
        s.lower = parseBool(p.getProperty("lower"), s.lower);
        s.digits = parseBool(p.getProperty("digits"), s.digits);
        s.symbols = parseBool(p.getProperty("symbols"), s.symbols);
        s.excludeAmbiguous = parseBool(p.getProperty("excludeAmbiguous"), s.excludeAmbiguous);
        s.theme = Theme.byName(p.getProperty("theme"), s.theme);
        return s;
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("length", String.valueOf(defaultLength));
        p.setProperty("upper", String.valueOf(upper));
        p.setProperty("lower", String.valueOf(lower));
        p.setProperty("digits", String.valueOf(digits));
        p.setProperty("symbols", String.valueOf(symbols));
        p.setProperty("excludeAmbiguous", String.valueOf(excludeAmbiguous));
        p.setProperty("theme", theme.name());
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "warden settings — no secrets here, safe to read");
        } catch (IOException e) {
            System.err.println("Не удалось сохранить настройки: " + e.getMessage());
        }
    }

    public PasswordGenerator.Options toOptions() {
        PasswordGenerator.Options o = new PasswordGenerator.Options();
        o.length = defaultLength;
        o.upper = upper;
        o.lower = lower;
        o.digits = digits;
        o.symbols = symbols;
        o.excludeAmbiguous = excludeAmbiguous;
        return o;
    }

    private static int parseInt(String v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static boolean parseBool(String v, boolean def) {
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }
}
