package studdy.example.demo.user;

import java.util.Optional;

// Regra única de formato da foto de perfil, usada pelo upload e pela migração das fotos antigas.
final class ProfilePhotoFormat {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private ProfilePhotoFormat() {
    }

    static Optional<String> detectContentType(byte[] content) {
        if (startsWith(content, PNG_SIGNATURE)) {
            return Optional.of("image/png");
        }

        if (content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF) {
            return Optional.of("image/jpeg");
        }

        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }

        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }

        return true;
    }
}
