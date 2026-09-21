package warden;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class PasswordGenerator {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>/?";
    private static final String AMBIGUOUS = "Il1O0oS5B8|";

    private final SecureRandom rng = new SecureRandom();

    public static final class Options {
        public int length = 16;
        public boolean upper = true;
        public boolean lower = true;
        public boolean digits = true;
        public boolean symbols = true;
        public boolean excludeAmbiguous = false;
    }

    /** Generates a password, guaranteeing at least one char from each selected class. */
    public String generate(Options opt) {
        List<String> pools = new ArrayList<>();
        if (opt.upper) pools.add(strip(UPPER, opt.excludeAmbiguous));
        if (opt.lower) pools.add(strip(LOWER, opt.excludeAmbiguous));
        if (opt.digits) pools.add(strip(DIGITS, opt.excludeAmbiguous));
        if (opt.symbols) pools.add(strip(SYMBOLS, opt.excludeAmbiguous));

        if (pools.isEmpty()) {
            throw new IllegalArgumentException("Выбери хотя бы один набор символов.");
        }
        if (opt.length < pools.size()) {
            throw new IllegalArgumentException(
                    "Длина слишком мала для выбранных наборов (минимум " + pools.size() + ").");
        }

        StringBuilder combined = new StringBuilder();
        for (String p : pools) combined.append(p);

        char[] result = new char[opt.length];
        int idx = 0;

        // Guarantee representation of every selected class.
        for (String pool : pools) {
            result[idx++] = pool.charAt(rng.nextInt(pool.length()));
        }
        // Fill the rest from the combined pool.
        while (idx < opt.length) {
            result[idx++] = combined.charAt(rng.nextInt(combined.length()));
        }

        // Fisher-Yates shuffle so guaranteed chars aren't always up front.
        for (int i = result.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = result[i];
            result[i] = result[j];
            result[j] = tmp;
        }
        return new String(result);
    }

    private String strip(String pool, boolean excludeAmbiguous) {
        if (!excludeAmbiguous) return pool;
        StringBuilder sb = new StringBuilder();
        for (char c : pool.toCharArray()) {
            if (AMBIGUOUS.indexOf(c) < 0) sb.append(c);
        }
        return sb.toString();
    }
}
