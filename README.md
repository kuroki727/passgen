<div align="center">

# passgen

A terminal password generator with a TUI and encrypted memory.<br>
Zero external dependencies — JDK standard library only.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/java-17%2B-orange.svg)
![Dependencies](https://img.shields.io/badge/dependencies-0-brightgreen.svg)
![Crypto](https://img.shields.io/badge/vault-AES--256--GCM-lightgrey.svg)

**[English](#english)** · **[Русский](#русский)**

</div>

---

<a name="english"></a>
## English

#### Contents
[Features](#features) · [Build & run](#build--run) · [Data storage](#data-storage) · [Project structure](#project-structure) · [License](#license)

### Features

- **Generation** — length, character classes (upper/lower/digits/symbols),
  option to exclude look-alike characters (`Il1O0o...`), guaranteed
  representation from every selected class, `SecureRandom`-based shuffle.
- **Memory** — encrypted password history with labels. **AES-256-GCM**,
  key derived from a master password via **PBKDF2WithHmacSHA256**
  (210k iterations). Everything is `javax.crypto` / `java.security` —
  standard JDK, no third-party crypto library.
- **Settings** (default length, character sets, theme) are stored
  separately, in plain text — no passwords ever touch that file.
- **5 themes** (ANSI 256-color): Everforest, Gruvbox, Nord, Dracula, Solarized.
- **Optional clipboard copy** (`java.awt.Toolkit`) — works with a graphical
  session (X11/Wayland); fails gracefully with a warning otherwise.

### Build & run

Requires JDK 17+ (uses switch expressions, `Path.of`, `String.repeat`).

```sh
make run          # build and run
# or by hand:
javac -d out src/passgen/*.java
java -cp out passgen.Main

# a runnable jar:
make jar
java -jar passgen.jar
```

### Data storage

- `~/.passgen/vault.dat` — encrypted history
  (`PGN1` magic + 16-byte salt + 12-byte IV + AES-GCM ciphertext).
- `~/.passgen/settings.properties` — plain-text settings.

The master password is never stored and can't be recovered — forget it and
the history stays encrypted forever. That's intentional, not a bug.

### Project structure

```
src/passgen/
  Main.java              — TUI loop, all flows (generate/history/settings)
  PasswordGenerator.java — generation logic
  Vault.java              — AES-GCM encryption/decryption of history
  VaultEntry.java          — one history record (label, password, date)
  VaultException.java      — vault error type
  Settings.java             — persisted settings (java.util.Properties)
  Theme.java                — ANSI themes
  ConsoleUI.java             — TUI helpers (frames, prompts, hidden input)
```

### License

MIT — see [LICENSE](LICENSE).

<div align="right"><a href="#passgen">↑ top</a></div>

---

<a name="русский"></a>
## Русский

#### Оглавление
[Возможности](#возможности) · [Сборка и запуск](#сборка-и-запуск) · [Хранение данных](#хранение-данных) · [Структура](#структура) · [Лицензия](#лицензия)

### Возможности

- **Генерация** — длина, наборы символов (заглавные/строчные/цифры/символы),
  исключение похожих символов (`Il1O0o...`), гарантия по одному символу
  из каждого выбранного набора, перемешивание через `SecureRandom`.
- **Память** — зашифрованная история паролей с метками. **AES-256-GCM**,
  ключ получается из мастер-пароля через **PBKDF2WithHmacSHA256**
  (210k итераций). Всё это `javax.crypto` / `java.security` из самого
  JDK — сторонней криптобиблиотеки нет.
- **Настройки** (длина по умолчанию, наборы символов, тема) хранятся
  отдельно, открытым текстом — паролей в этом файле нет.
- **5 тем** (ANSI 256 цветов): Everforest, Gruvbox, Nord, Dracula, Solarized.
- **Опциональное копирование в буфер** (`java.awt.Toolkit`) — работает при
  наличии графической сессии (X11/Wayland), иначе просто предупреждение
  без падения.

### Сборка и запуск

Нужен JDK 17+ (switch-выражения, `Path.of`, `String.repeat`).

```sh
make run          # собрать и запустить
# или вручную:
javac -d out src/passgen/*.java
java -cp out passgen.Main

# раннабельный jar:
make jar
java -jar passgen.jar
```

### Хранение данных

- `~/.passgen/vault.dat` — зашифрованная история
  (magic `PGN1` + salt(16) + iv(12) + AES-GCM ciphertext).
- `~/.passgen/settings.properties` — настройки, обычный текст.

Мастер-пароль нигде не хранится и не может быть восстановлен — забыл его,
значит история не расшифровывается. Это осознанный компромисс, а не баг.

### Структура

```
src/passgen/
  Main.java              — TUI-цикл, все сценарии (генерация/история/настройки)
  PasswordGenerator.java — сама генерация
  Vault.java              — AES-GCM шифрование/расшифровка истории
  VaultEntry.java          — запись истории (метка, пароль, дата)
  VaultException.java      — ошибки хранилища
  Settings.java             — persist настроек (java.util.Properties)
  Theme.java                — ANSI-темы
  ConsoleUI.java             — хелперы TUI (рамки, промпты, скрытый ввод пароля)
```

### Лицензия

MIT — см. [LICENSE](LICENSE).

<div align="right"><a href="#passgen">↑ наверх</a></div>
