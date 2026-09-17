package studdy.example.demo.avatar;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.avatar.dto.AvatarResponse;
import studdy.example.demo.user.UserRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class AvatarService {

    private final UserAvatarRepository avatarRepository;
    private final UserRepository userRepository;
    private final AvatarImageProcessor imageProcessor;

    public AvatarService(
            UserAvatarRepository avatarRepository,
            UserRepository userRepository,
            AvatarImageProcessor imageProcessor
    ) {
        this.avatarRepository = avatarRepository;
        this.userRepository = userRepository;
        this.imageProcessor = imageProcessor;
    }

    @Transactional
    public AvatarResponse upload(UUID userId, MultipartFile file) {
        byte[] content = imageProcessor.toAvatarJpeg(readUpload(file));
        Optional<UserAvatar> current = avatarRepository.findByUser_Id(userId);

        UserAvatar avatar;
        if (current.isPresent()) {
            avatar = current.get();
            avatar.replace(content, AvatarImageProcessor.OUTPUT_CONTENT_TYPE);
        } else {
            // O usuário vem do token já validado pelo filtro, então a referência dispensa um select.
            avatar = new UserAvatar(
                    userRepository.getReferenceById(userId),
                    content,
                    AvatarImageProcessor.OUTPUT_CONTENT_TYPE
            );
        }

        try {
            return AvatarResponse.from(avatarRepository.saveAndFlush(avatar));
        } catch (DataIntegrityViolationException exception) {
            // Duas abas enviaram a primeira foto ao mesmo tempo e a outra gravou antes.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sua foto acabou de ser enviada em outra aba. Tente novamente."
            );
        }
    }

    @Transactional(readOnly = true)
    public Optional<AvatarImage> find(UUID userId) {
        return avatarRepository.findByUser_Id(userId)
                .map(avatar -> new AvatarImage(avatar.getContent(), avatar.getContentType(), avatar.getUpdatedAt()));
    }

    @Transactional
    public void delete(UUID userId) {
        avatarRepository.deleteByUser_Id(userId);
    }

    // O tamanho é conferido antes de ler os bytes, para não carregar um arquivo enorme na memória.
    private byte[] readUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AvatarImageProcessor.EMPTY_UPLOAD_MESSAGE);
        }

        if (file.getSize() > AvatarImageProcessor.MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, AvatarImageProcessor.TOO_LARGE_MESSAGE);
        }

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler a imagem enviada.");
        }
    }
}
