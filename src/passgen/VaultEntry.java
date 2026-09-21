package passgen;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class VaultEntry {
    public final String label;
    public final String password;
    public final long createdAtEpochMs;

    public VaultEntry(String label, String password, long createdAtEpochMs) {
        this.label = label;
        this.password = password;
        this.createdAtEpochMs = createdAtEpochMs;
    }

    public String formattedDate() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(createdAtEpochMs));
    }

    public String masked() {
        if (password.length() <= 2) return "*".repeat(password.length());
        return password.charAt(0) + "*".repeat(password.length() - 2) + password.charAt(password.length() - 1);
    }
}
