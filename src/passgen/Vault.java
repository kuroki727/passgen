package passgen;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Encrypted-at-rest password history. AES-256-GCM with a PBKDF2-derived key —
 * everything used here (javax.crypto, java.security) ships in the JDK, so
 * there's still zero external dependencies.
 */
public final class Vault {
    private static final byte[] MAGIC = "PGN1".getBytes(StandardCharsets.US_ASCII);
    private static final String CHECK_LINE = "PASSGEN-CHECK";
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;

    private final Path file;
    private final byte[] salt;
    private byte[] key; // derived AES key bytes
    private final List<VaultEntry> entries = new ArrayList<>();
    private final SecureRandom rng = new SecureRandom();

    private Vault(Path file, byte[] salt, byte[] key) {
        this.file = file;
        this.salt = salt;
        this.key = key;
    }

    public static Vault create(Path file, char[] masterPassword) throws VaultException {
        byte[] salt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] key = deriveKey(masterPassword, salt);
        Vault v = new Vault(file, salt, key);
        v.save();
        return v;
    }

    public static Vault open(Path file, char[] masterPassword) throws VaultException {
        try {
            byte[] all = Files.readAllBytes(file);
            if (all.length < MAGIC.length + SALT_LEN + IV_LEN) {
                throw new VaultException("Файл хранилища повреждён.");
            }
            int pos = 0;
            for (byte b : MAGIC) {
                if (all[pos++] != b) throw new VaultException("Неверный формат файла хранилища.");
            }
            byte[] salt = new byte[SALT_LEN];
            System.arraycopy(all, pos, salt, 0, SALT_LEN);
            pos += SALT_LEN;
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(all, pos, iv, 0, IV_LEN);
            pos += IV_LEN;
            byte[] ciphertext = new byte[all.length - pos];
            System.arraycopy(all, pos, ciphertext, 0, ciphertext.length);

            byte[] key = deriveKey(masterPassword, salt);
            byte[] plain = aesGcmDecrypt(key, iv, ciphertext);
            String text = new String(plain, StandardCharsets.UTF_8);
            String[] lines = text.split("\n", -1);
            if (lines.length == 0 || !lines[0].equals(CHECK_LINE)) {
                throw new VaultException("Неверный мастер-пароль.");
            }

            Vault v = new Vault(file, salt, key);
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].isEmpty()) continue;
                String[] parts = lines[i].split("\t", 3);
                if (parts.length != 3) continue;
                String label = new String(Base64.getDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                String password = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                long ts = Long.parseLong(parts[2]);
                v.entries.add(new VaultEntry(label, password, ts));
            }
            return v;
        } catch (IOException e) {
            throw new VaultException("Не удалось прочитать файл хранилища: " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new VaultException("Неверный мастер-пароль.", e);
        }
    }

    public List<VaultEntry> entries() {
        return entries;
    }

    public void addEntry(VaultEntry e) throws VaultException {
        entries.add(e);
        save();
    }

    public void removeEntry(int index) throws VaultException {
        entries.remove(index);
        save();
    }

    /** Re-derives the key under a new master password and re-encrypts everything. */
    public void changeMasterPassword(char[] newPassword) throws VaultException {
        byte[] newSalt = new byte[SALT_LEN];
        rng.nextBytes(newSalt);
        byte[] newKey = deriveKey(newPassword, newSalt);
        System.arraycopy(newSalt, 0, salt, 0, SALT_LEN);
        this.key = newKey;
        save();
    }

    public void save() throws VaultException {
        try {
            StringBuilder sb = new StringBuilder(CHECK_LINE);
            for (VaultEntry e : entries) {
                sb.append('\n');
                sb.append(Base64.getEncoder().encodeToString(e.label.getBytes(StandardCharsets.UTF_8)));
                sb.append('\t');
                sb.append(Base64.getEncoder().encodeToString(e.password.getBytes(StandardCharsets.UTF_8)));
                sb.append('\t');
                sb.append(e.createdAtEpochMs);
            }
            byte[] iv = new byte[IV_LEN];
            rng.nextBytes(iv);
            byte[] ciphertext = aesGcmEncrypt(key, iv, sb.toString().getBytes(StandardCharsets.UTF_8));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(MAGIC);
            out.write(salt);
            out.write(iv);
            out.write(ciphertext);

            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.write(tmp, out.toByteArray());
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | GeneralSecurityException e) {
            throw new VaultException("Не удалось сохранить хранилище: " + e.getMessage(), e);
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt) throws VaultException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return key;
        } catch (GeneralSecurityException e) {
            throw new VaultException("Ошибка получения ключа: " + e.getMessage(), e);
        }
    }

    private static byte[] aesGcmEncrypt(byte[] key, byte[] iv, byte[] plaintext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(plaintext);
    }

    private static byte[] aesGcmDecrypt(byte[] key, byte[] iv, byte[] ciphertext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(ciphertext);
    }
}
