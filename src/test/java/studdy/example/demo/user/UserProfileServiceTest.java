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

    private static final String PASSWORD = "Senha@1234";

    private static final byte[] PNG = PhotoFixtures.png(32, 32);

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfilePhotoRepository photoRepository;

    @Autowired
    private UserProfileService profileService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser(
                "Gabriel Silva",
                "perfil-gabriel@example.com",
                passwordEncoder.encode(PASSWORD)
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
                request("  Gabriel Souza  ", "  GABRIEL.NOVO@EXAMPLE.COM  ", "  Gabriel.Souza  ", PASSWORD)
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
                new UpdateProfileRequest("Gabriel", user.getEmail(), "", "", null, null, "", null)
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
                        request("Gabriel", other.getEmail(), "gabrielsilva", PASSWORD)
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
    void storesAPngUploadReencodedAsJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "perfil.png", "image/png", PNG);

        UserResponse response = profileService.updatePhoto(user.getId(), file);
        ProfilePhotoContent storedPhoto = profileService.findPhoto(user.getId());

        assertTrue(response.hasProfilePhoto());
        assertEquals("/api/v1/users/me/profile-photo", response.profilePhotoUrl());
        assertEquals("image/jpeg", storedPhoto.contentType());
        assertEquals((byte) 0xFF, storedPhoto.content()[0]);
    }

    @Test
    void detectsTheRealImageTypeInsteadOfTrustingTheRequestHeader() {
        byte[] jpeg = PhotoFixtures.jpeg(20, 20);
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
    void rejectsUndecodableImageWithBadRequest() {
        byte[] corrupt = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", corrupt);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updatePhoto(user.getId(), file)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertFalse(photoRepository.existsByUser_Id(user.getId()));
    }

    @Test
    void stripsExifFromStoredPhoto() {
        byte[] jpeg = PhotoFixtures.withExif(PhotoFixtures.jpeg(40, 40));

        profileService.updatePhoto(user.getId(), new MockMultipartFile("file", "a.jpg", "image/jpeg", jpeg));

        assertFalse(PhotoFixtures.containsApp1(profileService.findPhoto(user.getId()).content()));
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
        byte[] jpeg = PhotoFixtures.jpeg(24, 24);

        profileService.updatePhoto(
                user.getId(),
                new MockMultipartFile("file", "perfil.jpg", "image/jpeg", jpeg)
        );

        assertEquals(1, photoRepository.count());
        assertEquals("image/jpeg", profileService.findPhoto(user.getId()).contentType());

        profileService.deletePhoto(user.getId());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.findPhoto(user.getId())
        );
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private UpdateProfileRequest request(String name, String email, String username) {
        return request(name, email, username, null);
    }

    @Test
    void changingEmailWithoutPasswordIsRejected() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.update(user.getId(), request("Gabriel", "novo-sem-senha@example.com", "gabrielsilva"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Informe sua senha atual para confirmar.", error.getReason());
        assertEquals("perfil-gabriel@example.com", userRepository.findById(user.getId()).orElseThrow().getEmail());
    }

    @Test
    void changingEmailWithWrongPasswordIsRejected() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> profileService.update(
                        user.getId(),
                        request("Gabriel", "novo-errada@example.com", "gabrielsilva", "senha-errada-000")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("A senha atual está incorreta.", error.getReason());
    }

    @Test
    void changingOtherFieldsWithoutChangingEmailDoesNotRequirePassword() {
        UserResponse response = profileService.update(
                user.getId(),
                request("Novo Nome", "  PERFIL-GABRIEL@example.com ", "gabrielsilva")
        );

        assertEquals("Novo Nome", response.name());
        assertEquals("perfil-gabriel@example.com", response.email());
    }

    private UpdateProfileRequest request(String name, String email, String username, String password) {
        return new UpdateProfileRequest(
                name,
                email,
                username,
                "(62) 99999-9999",
                LocalDate.of(2005, 3, 18),
                Gender.PREFER_NOT_TO_SAY,
                "  Goiânia - GO  ",
                password
        );
    }
}
