package passgen;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Small terminal-UI toolkit: no external deps, just ANSI + JDK I/O. */
public final class ConsoleUI {
    private final BufferedReader in;
    private final Console console;
    Theme theme;

    public ConsoleUI(Theme theme) {
        this.theme = theme;
        this.console = System.console();
        this.in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    public void header(String title) {
        String bar = "─".repeat(Math.max(2, title.length() + 4));
        System.out.println(theme.accent("┌" + bar + "┐"));
        System.out.println(theme.accent("│  ") + theme.bold(title) + theme.accent("  │"));
        System.out.println(theme.accent("└" + bar + "┘"));
    }

    public void rule() {
        System.out.println(theme.muted("─".repeat(48)));
    }

    public void info(String s) { System.out.println(theme.text(s)); }
    public void muted(String s) { System.out.println(theme.muted(s)); }
    public void ok(String s) { System.out.println(theme.ok(s)); }
    public void warn(String s) { System.out.println(theme.warn(s)); }
    public void blank() { System.out.println(); }

    public String prompt(String label) {
        System.out.print(theme.text(label));
        try {
            String line = in.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException e) {
            return "";
        }
    }

    public String promptDefault(String label, String def) {
        String v = prompt(label + theme.muted(" [" + def + "]: "));
        return v.isEmpty() ? def : v;
    }

    public int promptInt(String label, int def) {
        while (true) {
            String v = promptDefault(label, String.valueOf(def));
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                warn("Введи целое число, бро.");
            }
        }
    }

    public boolean promptYesNo(String label, boolean def) {
        String suffix = def ? " [Y/n]: " : " [y/N]: ";
        String v = prompt(label + theme.muted(suffix)).toLowerCase();
        if (v.isEmpty()) return def;
        return v.startsWith("y") || v.equals("д") || v.equals("да");
    }

    /** Reads a password without echoing it, falling back to plain input if no TTY is attached. */
    public char[] promptPassword(String label) {
        if (console != null) {
            char[] pw = console.readPassword(theme.text(label));
            return pw == null ? new char[0] : pw;
        }
        warn("(нет TTY — ввод будет отображаться в открытую)");
        String v = prompt(label);
        return v.toCharArray();
    }

    public void pause() {
        prompt(theme.muted("Enter — продолжить... "));
    }
}
