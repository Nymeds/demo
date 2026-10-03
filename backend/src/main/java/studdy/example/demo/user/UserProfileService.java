package studdy.example.demo.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.settings.AccountCredentials;
import studdy.example.demo.user.dto.UpdateProfileRequest;
import studdy.example.demo.user.dto.UserResponse;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfilePhotoRepository photoRepository;
    private final AccountCredentials accountCredentials;
    private final JwtService jwtService;

    public UserProfileService(
            UserRepository userRepository,
            UserProfilePhotoRepository photoRepository,
            AccountCredentials accountCredentials,
            JwtService jwtService
    ) {
        this.jwtService = jwtService;
        this.accountCredentials = accountCredentials;
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
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        // O e-mail é o login: trocá-lo exige a senha atual (mesma regra das configurações) e trava
        // o usuário, para uma renovação de sessão simultânea esperar a troca terminar.
        boolean emailChanged = !email.equals(findUser(userId).getEmail());
        AppUser user = emailChanged ? accountCredentials.findUserForUpdate(userId) : findUser(userId);
        if (emailChanged) {
            accountCredentials.requireCurrentPassword(user, request.currentPassword());
        }

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
                normalizePhone(request.phone()),
                request.birthDate(),
                request.gender(),
                normalizeOptional(request.location())
        );

        if (emailChanged) {
            user.markCredentialsChanged();
        }

        try {
            UserResponse response = toResponse(userRepository.saveAndFlush(user));

            // Todas as sessões caem (inclusive access tokens antigos); quem trocou recebe um token novo
            // e o UserController troca o cookie da sessão deste navegador.
            return emailChanged ? response.withSession(new AuthResponse(
                    jwtService.generateTokenFor(user.getId(), user.getCredentialsUpdatedAt()),
                    "Bearer",
                    jwtService.getExpirationInSeconds()
            )) : response;
        } catch (DataIntegrityViolationException exception) {
            throw conflict("O e-mail ou nome de usuário informado já está em uso.");
        }
    }

    @Transactional
    public UserResponse updatePhoto(UUID userId, MultipartFile file) {
        byte[] rawContent = readAndValidate(file);
        requireSupportedFormat(rawContent);
        // Trava a linha do usuário: dois envios simultâneos não tentam inserir duas fotos.
        AppUser user = accountCredentials.findUserForUpdate(userId);
        byte[] content = ProfilePhotoProcessor.process(rawContent).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Não foi possível ler a imagem. Envie uma foto PNG ou JPG válida."
        ));
        String contentType = ProfilePhotoProcessor.CONTENT_TYPE;

        photoRepository.findByUser_Id(userId).ifPresentOrElse(
                photo -> photo.update(content, contentType),
                () -> photoRepository.save(new UserProfilePhoto(user, content, contentType)));
        user.markProfileUpdated();
        try {
            photoRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw conflict("A foto de perfil foi alterada ao mesmo tempo em outra aba. Tente novamente.");
        }

        return UserResponse.from(user, true);
    }

    @Transactional(readOnly = true)
    public ProfilePhotoContent findPhoto(UUID userId) {
        findUser(userId);
        return photoRepository.findByUser_Id(userId)
                .map(photo -> new ProfilePhotoContent(photo.getContent(), photo.getContentType()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Foto de perfil não cadastrada."
                ));
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

    private void requireSupportedFormat(byte[] content) {
        ProfilePhotoFormat.detectContentType(content).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "A foto de perfil deve estar no formato PNG ou JPG."
        ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        String normalized = normalizeOptional(username);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        return value == null || value.isBlank() ? null : PhoneNumbers.digitsOf(value);
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
