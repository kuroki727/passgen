package passgen;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class Main {
    private static Path homeDir;
    private static Path vaultFile;
    private static Path settingsFile;
    private static Settings settings;
    private static Vault vault;
    private static ConsoleUI ui;
    private static final PasswordGenerator GEN = new PasswordGenerator();

    public static void main(String[] args) throws IOException {
        // Force UTF-8 for cyrillic UI text regardless of the host's default locale.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        homeDir = Path.of(System.getProperty("user.home"), ".passgen");
        Files.createDirectories(homeDir);
        vaultFile = homeDir.resolve("vault.dat");
        settingsFile = homeDir.resolve("settings.properties");

        settings = Settings.load(settingsFile);
        ui = new ConsoleUI(settings.theme);

        ui.header("PASSGEN — генератор и хранилище паролей");
        ui.blank();

        if (!Files.exists(vaultFile)) {
            firstRun();
        } else {
            unlock();
        }
        if (vault == null) {
            ui.warn("Не получилось открыть хранилище. Выход.");
            return;
        }

        mainLoop();
    }

    private static void firstRun() {
        ui.info("Похоже, это первый запуск. Задай мастер-пароль — он шифрует твою историю паролей (AES-256-GCM).");
        ui.muted("Если забудешь его, история будет не восстановить — это by design.");
        while (true) {
            char[] p1 = ui.promptPassword("Мастер-пароль: ");
            char[] p2 = ui.promptPassword("Повтори: ");
            boolean match = Arrays.equals(p1, p2);
            wipe(p2);
            if (!match) {
                wipe(p1);
                ui.warn("Пароли не совпадают, попробуй ещё раз.");
                continue;
            }
            if (p1.length < 4) {
                wipe(p1);
                ui.warn("Слишком короткий, возьми хотя бы 4 символа.");
                continue;
            }
            try {
                vault = Vault.create(vaultFile, p1);
                ui.ok("Хранилище создано: " + vaultFile);
            } catch (VaultException e) {
                ui.warn(e.getMessage());
            } finally {
                wipe(p1);
            }
            return;
        }
    }

    private static void unlock() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            char[] pw = ui.promptPassword("Мастер-пароль: ");
            try {
                vault = Vault.open(vaultFile, pw);
                return;
            } catch (VaultException e) {
                ui.warn(e.getMessage() + " (" + attempt + "/3)");
            } finally {
                wipe(pw);
            }
        }
    }

    private static void mainLoop() {
        while (true) {
            ui.blank();
            ui.rule();
            ui.info("1) Сгенерировать пароль");
            ui.info("2) История (" + vault.entries().size() + ")");
            ui.info("3) Настройки");
            ui.info("4) Выход");
            ui.rule();
            String choice = ui.prompt("> ");
            switch (choice) {
                case "1" -> generateFlow();
                case "2" -> historyFlow();
                case "3" -> settingsFlow();
                case "4" -> { ui.muted("До связи."); return; }
                default -> ui.warn("Не понял, выбери 1-4.");
            }
        }
    }

    private static void generateFlow() {
        ui.blank();
        ui.header("Генерация пароля");

        PasswordGenerator.Options opt = new PasswordGenerator.Options();
        opt.length = ui.promptInt("Длина", settings.defaultLength);

        if (ui.promptYesNo("Использовать наборы символов по умолчанию (A-Z a-z 0-9 !@#...)", true)) {
            opt.upper = settings.upper;
            opt.lower = settings.lower;
            opt.digits = settings.digits;
            opt.symbols = settings.symbols;
            opt.excludeAmbiguous = settings.excludeAmbiguous;
        } else {
            opt.upper = ui.promptYesNo("Заглавные буквы", settings.upper);
            opt.lower = ui.promptYesNo("Строчные буквы", settings.lower);
            opt.digits = ui.promptYesNo("Цифры", settings.digits);
            opt.symbols = ui.promptYesNo("Символы", settings.symbols);
            opt.excludeAmbiguous = ui.promptYesNo("Исключить похожие символы (Il1O0o...)", settings.excludeAmbiguous);
        }

        String password;
        try {
            password = GEN.generate(opt);
        } catch (IllegalArgumentException e) {
            ui.warn(e.getMessage());
            return;
        }

        ui.blank();
        ui.info("Пароль:  " + settings.theme.bold(password));
        ui.blank();

        if (ui.promptYesNo("Скопировать в буфер обмена", false)) {
            copyToClipboard(password);
        }

        if (ui.promptYesNo("Сохранить в историю", true)) {
            String label = ui.promptDefault("Метка (например, github, wifi)", "без метки");
            try {
                vault.addEntry(new VaultEntry(label, password, System.currentTimeMillis()));
                ui.ok("Сохранено.");
            } catch (VaultException e) {
                ui.warn(e.getMessage());
            }
        }
    }

    private static void historyFlow() {
        while (true) {
            ui.blank();
            ui.header("История");
            List<VaultEntry> entries = vault.entries();
            if (entries.isEmpty()) {
                ui.muted("Пока пусто.");
                ui.pause();
                return;
            }
            for (int i = 0; i < entries.size(); i++) {
                VaultEntry e = entries.get(i);
                ui.info((i + 1) + ") " + e.label + "  " + settings.theme.muted(e.formattedDate() + "  " + e.masked()));
            }
            ui.blank();
            ui.muted("Номер — показать пароль, d<номер> — удалить (напр. d2), Enter — назад.");
            String choice = ui.prompt("> ");
            if (choice.isEmpty()) return;

            if (choice.toLowerCase().startsWith("d")) {
                Integer idx = parseIndex(choice.substring(1), entries.size());
                if (idx == null) { ui.warn("Неверный номер."); continue; }
                if (ui.promptYesNo("Удалить \"" + entries.get(idx).label + "\"", false)) {
                    try {
                        vault.removeEntry(idx);
                        ui.ok("Удалено.");
                    } catch (VaultException e) {
                        ui.warn(e.getMessage());
                    }
                }
                continue;
            }

            Integer idx = parseIndex(choice, entries.size());
            if (idx == null) { ui.warn("Неверный номер."); continue; }
            VaultEntry e = entries.get(idx);
            ui.info("Пароль: " + settings.theme.bold(e.password));
            if (ui.promptYesNo("Скопировать в буфер обмена", false)) {
                copyToClipboard(e.password);
            }
        }
    }

    private static Integer parseIndex(String s, int size) {
        try {
            int i = Integer.parseInt(s.trim()) - 1;
            return (i >= 0 && i < size) ? i : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void settingsFlow() {
        while (true) {
            ui.blank();
            ui.header("Настройки");
            ui.info("1) Длина по умолчанию: " + settings.defaultLength);
            ui.info("2) Заглавные: " + settings.upper);
            ui.info("3) Строчные: " + settings.lower);
            ui.info("4) Цифры: " + settings.digits);
            ui.info("5) Символы: " + settings.symbols);
            ui.info("6) Исключать похожие символы: " + settings.excludeAmbiguous);
            ui.info("7) Тема: " + settings.theme.label);
            ui.info("8) Сменить мастер-пароль");
            ui.info("9) Назад");
            String choice = ui.prompt("> ");
            switch (choice) {
                case "1" -> settings.defaultLength = ui.promptInt("Новая длина", settings.defaultLength);
                case "2" -> settings.upper = ui.promptYesNo("Заглавные буквы", settings.upper);
                case "3" -> settings.lower = ui.promptYesNo("Строчные буквы", settings.lower);
                case "4" -> settings.digits = ui.promptYesNo("Цифры", settings.digits);
                case "5" -> settings.symbols = ui.promptYesNo("Символы", settings.symbols);
                case "6" -> settings.excludeAmbiguous = ui.promptYesNo("Исключать похожие символы", settings.excludeAmbiguous);
                case "7" -> chooseTheme();
                case "8" -> changeMasterPassword();
                case "9" -> { settings.save(); return; }
                default -> ui.warn("Не понял, выбери 1-9.");
            }
            settings.save();
        }
    }

    private static void chooseTheme() {
        Theme[] all = Theme.values();
        for (int i = 0; i < all.length; i++) {
            ui.info((i + 1) + ") " + all[i].label);
        }
        Integer idx = parseIndex(ui.prompt("> "), all.length);
        if (idx != null) {
            settings.theme = all[idx];
            ui.theme = settings.theme;
            ui.ok("Тема: " + settings.theme.label);
        }
    }

    private static void changeMasterPassword() {
        char[] p1 = ui.promptPassword("Новый мастер-пароль: ");
        char[] p2 = ui.promptPassword("Повтори: ");
        try {
            if (!Arrays.equals(p1, p2)) {
                ui.warn("Пароли не совпадают.");
                return;
            }
            if (p1.length < 4) {
                ui.warn("Слишком короткий.");
                return;
            }
            vault.changeMasterPassword(p1);
            ui.ok("Мастер-пароль изменён, хранилище перешифровано.");
        } catch (VaultException e) {
            ui.warn(e.getMessage());
        } finally {
            wipe(p1);
            wipe(p2);
        }
    }

    private static void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            ui.ok("Скопировано.");
        } catch (HeadlessException | IllegalStateException e) {
            ui.warn("Буфер обмена недоступен (нет графической сессии).");
        }
    }

    private static void wipe(char[] arr) {
        if (arr != null) Arrays.fill(arr, '\0');
    }
}
