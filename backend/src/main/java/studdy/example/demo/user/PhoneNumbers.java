package studdy.example.demo.user;

import java.util.regex.Pattern;

/** Regras do celular brasileiro: (DD) 9XXXX-XXXX, guardado somente com os 11 dígitos. */
public final class PhoneNumbers {

    private static final Pattern MOBILE = Pattern.compile("^[1-9][1-9]9\\d{8}$");
    private static final int MAX_DIGITS = 11;

    private PhoneNumbers() {
    }

    /** Remove tudo que não é dígito; descarta o 55 inicial só quando há 13 dígitos. */
    public static String digitsOf(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 13 && digits.startsWith("55")) {
            digits = digits.substring(2);
        }
        return digits;
    }

    /** Vazio é válido (campo opcional). */
    public static boolean isValidOrBlank(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String digits = digitsOf(raw);
        return digits.length() == MAX_DIGITS && MOBILE.matcher(digits).matches();
    }
}
