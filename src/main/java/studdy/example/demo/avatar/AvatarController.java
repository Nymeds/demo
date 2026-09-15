package studdy.example.demo.avatar;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import studdy.example.demo.avatar.dto.AvatarResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings/avatar")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarResponse upload(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file
    ) {
        return avatarService.upload(userId, file);
    }

    // Sem foto cadastrada responde 204, e o frontend mostra a inicial do nome no lugar.
    @GetMapping
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal UUID userId) {
        return avatarService.find(userId)
                // Toda foto é regravada como JPEG pelo AvatarImageProcessor antes de ser salva.
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .lastModified(image.updatedAt())
                        .body(image.content()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId) {
        avatarService.delete(userId);
    }
}
