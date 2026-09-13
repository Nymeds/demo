package studdy.example.demo.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.user.dto.UpdateProfileRequest;
import studdy.example.demo.user.dto.UserResponse;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final UserRepository userRepository;
    private final UserProfilePhotoRepository photoRepository;

    public UserProfileService(
            UserRepository userRepository,
            UserProfilePhotoRepository photoRepository
    ) {
        this.userRepository = userRepository;
        this.photoRepository = photoRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse findCurrentUser(UUID userId) {
        AppUser user = findUser(userId);
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateProfileRequest request) {
        AppUser user = findUser(userId);
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw conflict("Já existe um usuário com este e-mail.");
        }

        if (username != null && userRepository.existsByUsernameAndIdNot(username, userId)) {
            throw conflict("Este nome de usuário já está em uso.");
        }

        user.updateProfile(
                request.name().trim(),
                email,
                username,
                normalizeOptional(request.phone()),
                request.birthDate(),
                request.gender(),
                normalizeOptional(request.location())
        );

        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("O e-mail ou nome de usuário informado já está em uso.");
        }
    }

    @Transactional
    public UserResponse updatePhoto(UUID userId, MultipartFile file) {
        AppUser user = findUser(userId);
        byte[] content = readAndValidate(file);
        String contentType = detectContentType(content);

        UserProfilePhoto photo = photoRepository.findByUser_Id(userId)
                .orElseGet(() -> new UserProfilePhoto(user, content, contentType));
        photo.update(content, contentType);
        photoRepository.save(photo);
        user.markProfileUpdated();

        return UserResponse.from(user, true);
    }

    @Transactional(readOnly = true)
    public ProfilePhotoContent findPhoto(UUID userId) {
        findUser(userId);
        UserProfilePhoto photo = photoRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Foto de perfil não cadastrada."
                ));

        return new ProfilePhotoContent(photo.getContent(), photo.getContentType());
    }

    @Transactional
    public void deletePhoto(UUID userId) {
        AppUser user = findUser(userId);

        if (!photoRepository.existsByUser_Id(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto de perfil não cadastrada.");
        }

        photoRepository.deleteByUser_Id(userId);
        user.markProfileUpdated();
    }

    private AppUser findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));
    }

    private UserResponse toResponse(AppUser user) {
        return UserResponse.from(user, photoRepository.existsByUser_Id(user.getId()));
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma foto de perfil.");
        }

        if (file.getSize() > UserProfilePhoto.MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.CONTENT_TOO_LARGE,
                    "A foto de perfil deve ter no máximo 2 MB."
            );
        }

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível processar a foto de perfil.",
                    exception
            );
        }
    }

    private String detectContentType(byte[] content) {
        if (startsWith(content, PNG_SIGNATURE)) {
            return "image/png";
        }

        if (content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "A foto de perfil deve estar no formato PNG ou JPG."
        );
    }

    private boolean startsWith(byte[] content, byte[] signature) {
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        String normalized = normalizeOptional(username);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
