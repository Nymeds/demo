package studdy.example.demo.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.settings.dto.ChangePasswordRequest;
import studdy.example.demo.settings.dto.ProfileResponse;
import studdy.example.demo.settings.dto.UpdateProfileRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ProfileSettingsServiceTest {

    private static final String PASSWORD = "senha-atual-123";
    private static final String EMAIL = "perfil@example.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProfileSettingsService profileSettingsService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser("Estudante", EMAIL, passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void returnsTheProfileWithoutAPasswordChangeYet() {
        ProfileResponse profile = profileSettingsService.find(user.getId());

        assertEquals(EMAIL, profile.email());
        assertNotNull(profile.createdAt());
        assertNull(profile.passwordChangedAt());
    }

    @Test
    void updatesTheNameWithoutAskingForThePassword() {
        ProfileResponse profile = profileSettingsService.update(
                user.getId(),
                new UpdateProfileRequest("  Maria Souza  ", EMAIL, null)
        );

        assertEquals("Maria Souza", profile.name());
        assertEquals(EMAIL, profile.email());
    }

    @Test
    void doesNotTreatADifferentCaseAsANewEmail() {
        ProfileResponse profile = profileSettingsService.update(
                user.getId(),
                new UpdateProfileRequest("Estudante", "  PERFIL@Example.com ", null)
        );

        assertEquals(EMAIL, profile.email());
    }

    @Test
    void asksForThePasswordToChangeTheEmail() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> profileSettingsService.update(
                user.getId(),
                new UpdateProfileRequest("Estudante", "novo@example.com", null)
        ));
    }

    @Test
    void rejectsAWrongPasswordWhenChangingTheEmail() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> profileSettingsService.update(
                user.getId(),
                new UpdateProfileRequest("Estudante", "novo@example.com", "senha-errada-000")
        ));

        assertEquals(EMAIL, userRepository.findById(user.getId()).orElseThrow().getEmail());
    }

    @Test
    void changesTheEmailWithTheCurrentPassword() {
        ProfileResponse profile = profileSettingsService.update(
                user.getId(),
                new UpdateProfileRequest("Estudante", "Novo@Example.com", PASSWORD)
        );

        assertEquals("novo@example.com", profile.email());
        assertTrue(userRepository.existsByEmail("novo@example.com"));
        assertFalse(userRepository.existsByEmail(EMAIL));
    }

    @Test
    void refusesAnEmailThatBelongsToAnotherAccount() {
        userRepository.save(new AppUser("Outra pessoa", "ocupado@example.com", "hash"));

        assertStatus(HttpStatus.CONFLICT, () -> profileSettingsService.update(
                user.getId(),
                new UpdateProfileRequest("Estudante", "ocupado@example.com", PASSWORD)
        ));
    }

    @Test
    void changesThePasswordAndReturnsAFreshToken() {
        AuthResponse response = profileSettingsService.changePassword(
                user.getId(),
                new ChangePasswordRequest(PASSWORD, "nova-senha-456")
        );

        AppUser reloaded = userRepository.findById(user.getId()).orElseThrow();

        assertTrue(passwordEncoder.matches("nova-senha-456", reloaded.getPasswordHash()));
        assertFalse(passwordEncoder.matches(PASSWORD, reloaded.getPasswordHash()));
        assertNotNull(reloaded.getCredentialsUpdatedAt());
        assertEquals(user.getId(), jwtService.getUserId(response.accessToken()));
        assertTrue(reloaded.acceptsTokenIssuedAt(jwtService.parse(response.accessToken()).issuedAt()));
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void treatsACurrentPasswordAboveTheBcryptLimitAsIncorrect() {
        // 40 caracteres acentuados passam do limite de 72 bytes; o BCrypt lançaria exceção (erro 500).
        assertStatus(HttpStatus.BAD_REQUEST, () -> profileSettingsService.changePassword(
                user.getId(),
                new ChangePasswordRequest("á".repeat(40), "nova-senha-456")
        ));
    }

    @Test
    void rejectsAPasswordChangeWithTheWrongCurrentPassword() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> profileSettingsService.changePassword(
                user.getId(),
                new ChangePasswordRequest("senha-errada-000", "nova-senha-456")
        ));

        assertNull(userRepository.findById(user.getId()).orElseThrow().getCredentialsUpdatedAt());
    }

    @Test
    void rejectsANewPasswordEqualToTheCurrentOne() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> profileSettingsService.changePassword(
                user.getId(),
                new ChangePasswordRequest(PASSWORD, PASSWORD)
        ));
    }

    private void assertStatus(HttpStatus expected, Runnable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action::run);

        assertEquals(expected, error.getStatusCode());
    }
}
