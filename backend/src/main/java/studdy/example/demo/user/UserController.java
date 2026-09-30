package studdy.example.demo.user;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import studdy.example.demo.user.dto.UpdateProfileRequest;
import studdy.example.demo.user.dto.UserResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService profileService;

    public UserController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UUID userId) {
        return profileService.findCurrentUser(userId);
    }

    @PutMapping("/me")
    public UserResponse update(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileService.update(userId, request);
    }

    @GetMapping("/me/profile-photo")
    public ResponseEntity<byte[]> profilePhoto(@AuthenticationPrincipal UUID userId) {
        ProfilePhotoContent photo = profileService.findPhoto(userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(photo.content());
    }

    @PutMapping(value = "/me/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateProfilePhoto(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file
    ) {
        return profileService.updatePhoto(userId, file);
    }

    @DeleteMapping("/me/profile-photo")
    public ResponseEntity<Void> deleteProfilePhoto(@AuthenticationPrincipal UUID userId) {
        profileService.deletePhoto(userId);
        return ResponseEntity.noContent().build();
    }
}
