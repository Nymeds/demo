package studdy.example.demo.security;

import java.nio.charset.StandardCharsets;

public final class PasswordLimits {

    /** O BCrypt só aceita 72 bytes e lança exceção acima disso. */
    public static final int BCRYPT_MAX_BYTES = 72;

    private PasswordLimits() {
    }

    public static boolean exceedsBcryptLimit(String rawPassword) {
        return rawPassword != null && rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
