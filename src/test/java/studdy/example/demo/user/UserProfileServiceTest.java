package studdy.example.demo.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.user.dto.UpdateProfileRequest;
import studdy.example.demo.user.dto.UserResponse;
import studdy.example.demo.avatar.UserAvatar;
import studdy.example.demo.avatar.UserAvatarRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserProfileServiceTest {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
    };

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfilePhotoRepository photoRepository;

    @Autowired
    private UserProfileService profileService;

    @Autowired
    private UserAvatarRepository legacyAvatarRepository;

    @Test
    void readsPhotoPreviouslySavedInSettings() {
        legacyAvatarRepository.saveAndFlush(new UserAvatar(user, PNG, "image/png"));

        assertTrue(profileService.findCurrentUser(user.getId()).hasProfilePhoto());
        assertArrayEquals(PNG, profileService.findPhoto(user.getId()).content());
    }

    @Test
    void replacingLegacyPhotoKeepsOnlyTheNewProfilePhoto() {
        legacyAvatarRepository.saveAndFlush(new UserAvatar(user, PNG, "image/png"));
        profileService.updatePhoto(user.getId(), new MockMultipartFile("file", "photo.png", "image/png", PNG));

        assertFalse(legacyAvatarRepository.existsByUser_Id(user.getId()));
        assertTrue(photoRepository.existsByUser_Id(user.getId()));
        assertTrue(profileService.findCurrentUser(user.getId()).hasProfilePhoto());
    }

    @Test
    void deletingProfilePhotoAlsoRemovesLegacyPhoto() {
        profileService.updatePhoto(user.getId(), new MockMultipartFile("file", "photo.png", "image/png", PNG));
        legacyAvatarRepository.saveAndFlush(new UserAvatar(user, PNG, "image/png"));

        profileService.deletePhoto(user.getId());

        assertFalse(legacyAvatarRepository.existsByUser_Id(user.getId()));
        assertFalse(photoRepository.existsByUser_Id(user.getId()));
        assertFalse(profileService.findCurrentUser(user.getId()).hasProfilePhoto());
    }

    @Test
    void deletingPhotoThatExistsOnlyInSettingsDoesNotRequireNewUpload() {
        legacyAvatarRepository.saveAndFlush(new UserAvatar(user, PNG, "image/png"));
        profileService.deletePhoto(user.getId());
        assertFalse(profileService.findCurrentUser(user.getId()).hasProfilePhoto());
    }

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser(
                "Gabriel Silva",
                "perfil-gabriel@example.com",
                "hash"
        ));
    }

    @Test
    void readsTheAuthenticatedUsersProfile() {
        UserResponse response = profileService.findCurrentUser(user.getId());

        assertEquals(user.getId(), response.id());
        assertEquals("Gabriel Silva", response.name());
        assertEquals("perfil-gabriel@example.com", response.email());
        assertFalse(response.hasProfilePhoto());
        assertNull(response.profilePhotoUrl());
    }

    @Test
    void updatesAndNormalizesTheProfile() {
        UserResponse response = profileService.update(
                user.getId(),
                request("  Gabriel Souza  ", "  GABRIEL.NOVO@EXAMPLE.COM  ", "  Gabriel.Souza  ")
        );

        assertEquals("Gabriel Souza", response.name());
        assertEquals("gabriel.novo@example.com", response.email());
        assertEquals("gabriel.souza", response.username());
        assertEquals("(62) 99999-9999", response.phone());
        assertEquals(LocalDate.of(2005, 3, 18), response.birthDate());
        assertEquals(Gender.PREFER_NOT_TO_SAY, response.gender());
        assertEquals("Goiânia - GO", response.location());
    }

    @Test
    void clearsOptionalTextFieldsWhenTheyAreBlank() {
        profileService.update(user.getId(), request("Gabriel", user.getEmail(), "gabrielsilva"));

        UserResponse response = profileService.update(
                user.getId(),
                new UpdateProfileRequest("Gabriel", user.getEmail(), "", "", null, null, "")
        );

        assertNull(response.username());
        assertNull(response.phone());
        assertNull(response.birthDate());
        assertNull(response.gender());
        assertNull(response.location());
    }

    @Test
    void rejectsAnEmailOwnedByAnotherUser() {
        AppUser other = userRepository.save(new AppUser("Outro", "outro-perfil@example.com", "hash"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.update(
                        user.getId(),
                        request("Gabriel", other.getEmail(), "gabrielsilva")
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void rejectsAUsernameOwnedByAnotherUser() {
        AppUser other = new AppUser("Outro", "outro-usuario@example.com", "hash");
        other.updateProfile(
                "Outro",
                other.getEmail(),
                "usuarioocupado",
                null,
                null,
                null,
                null
        );
        userRepository.save(other);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.update(
                        user.getId(),
                        request("Gabriel", user.getEmail(), "UsuarioOcupado")
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void storesAndReadsAPngProfilePhoto() {
        MockMultipartFile file = new MockMultipartFile("file", "perfil.png", "image/png", PNG);

        UserResponse response = profileService.updatePhoto(user.getId(), file);
        ProfilePhotoContent storedPhoto = profileService.findPhoto(user.getId());

        assertTrue(response.hasProfilePhoto());
        assertEquals("/api/v1/users/me/profile-photo", response.profilePhotoUrl());
        assertEquals("image/png", storedPhoto.contentType());
        assertArrayEquals(PNG, storedPhoto.content());
    }

    @Test
    void detectsTheRealImageTypeInsteadOfTrustingTheRequestHeader() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
        MockMultipartFile file = new MockMultipartFile("file", "perfil.png", "image/png", jpeg);

        profileService.updatePhoto(user.getId(), file);

        assertEquals("image/jpeg", profileService.findPhoto(user.getId()).contentType());
    }

    @Test
    void rejectsUnsupportedProfilePhotoContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "perfil.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes()
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updatePhoto(user.getId(), file)
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, error.getStatusCode());
        assertFalse(photoRepository.existsByUser_Id(user.getId()));
    }

    @Test
    void rejectsProfilePhotosLargerThanTwoMegabytes() {
        byte[] oversized = new byte[UserProfilePhoto.MAX_FILE_SIZE + 1];
        System.arraycopy(PNG, 0, oversized, 0, PNG.length);
        MockMultipartFile file = new MockMultipartFile("file", "grande.png", "image/png", oversized);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updatePhoto(user.getId(), file)
        );

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, error.getStatusCode());
    }

    @Test
    void replacesAndDeletesTheProfilePhoto() {
        profileService.updatePhoto(
                user.getId(),
                new MockMultipartFile("file", "perfil.png", "image/png", PNG)
        );
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x02};

        profileService.updatePhoto(
                user.getId(),
                new MockMultipartFile("file", "perfil.jpg", "image/jpeg", jpeg)
        );

        assertEquals(1, photoRepository.count());
        assertArrayEquals(jpeg, profileService.findPhoto(user.getId()).content());

        profileService.deletePhoto(user.getId());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.findPhoto(user.getId())
        );
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private UpdateProfileRequest request(String name, String email, String username) {
        return new UpdateProfileRequest(
                name,
                email,
                username,
                "(62) 99999-9999",
                LocalDate.of(2005, 3, 18),
                Gender.PREFER_NOT_TO_SAY,
                "  Goiânia - GO  "
        );
    }
}
