package studdy.example.demo.settings;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.settings.dto.ChangePasswordRequest;
import studdy.example.demo.settings.dto.DeleteAccountRequest;
import studdy.example.demo.settings.dto.UpdatePreferencesRequest;
import studdy.example.demo.settings.dto.UpdateProfileRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAProfileWithoutPasswordWhenOnlyTheNameChanges() {
        assertTrue(validator.validate(new UpdateProfileRequest("Maria", "maria@example.com", null)).isEmpty());
    }

    @Test
    void requiresTheName() {
        assertRejects(new UpdateProfileRequest(" ", "maria@example.com", null), "Nome obrigatório.");
    }

    @Test
    void rejectsAnInvalidEmail() {
        assertRejects(new UpdateProfileRequest("Maria", "maria-sem-arroba", null), "E-mail inválido.");
    }

    @Test
    void rejectsAnEmailLongerThanTheColumn() {
        String email = "a".repeat(60) + "@" + "b".repeat(90) + ".com";

        assertRejects(new UpdateProfileRequest("Maria", email, null), "O e-mail deve ter no máximo 150 caracteres.");
    }

    @Test
    void rejectsAShortNewPassword() {
        assertRejects(
                new ChangePasswordRequest("senha-atual-1", "curta"),
                "A nova senha deve ter entre 8 e 72 caracteres."
        );
    }

    @Test
    void rejectsANewPasswordLongerThanBcryptAccepts() {
        assertRejects(
                new ChangePasswordRequest("senha-atual-1", "a".repeat(73)),
                "A nova senha deve ter entre 8 e 72 caracteres."
        );
    }

    @Test
    void rejectsAnAccentedNewPasswordThatFitsInCharactersButNotInBcryptBytes() {
        // 40 caracteres, mas 80 bytes em UTF-8.
        assertRejects(
                new ChangePasswordRequest("senha-atual-1", "á".repeat(40)),
                "A nova senha é longa demais. Letras acentuadas ocupam mais espaço; use menos caracteres."
        );
    }

    @Test
    void requiresTheCurrentPasswordToChangeIt() {
        assertRejects(new ChangePasswordRequest("", "nova-senha-123"), "Informe sua senha atual.");
    }

    @Test
    void requiresThePasswordToDeleteTheAccount() {
        assertRejects(new DeleteAccountRequest(""), "Informe sua senha atual para excluir a conta.");
    }

    @Test
    void acceptsPreferencesOnBothEdgesOfTheLimits() {
        assertTrue(validator.validate(new UpdatePreferencesRequest(1, 0, StartSection.DASHBOARD, null)).isEmpty());
        assertTrue(validator.validate(new UpdatePreferencesRequest(30, 30, StartSection.SIMULATOR, new BigDecimal("0"))).isEmpty());
        assertTrue(validator.validate(new UpdatePreferencesRequest(3, 10, StartSection.GRADES, new BigDecimal("10.0"))).isEmpty());
    }

    @Test
    void rejectsADeadlineAlertOfZeroDays() {
        assertRejects(
                new UpdatePreferencesRequest(0, 10, StartSection.DASHBOARD, null),
                "A antecedência dos avisos de prazo deve ser de pelo menos 1 dia."
        );
    }

    @Test
    void rejectsAnAttendanceMarginAboveThirtyPoints() {
        assertRejects(
                new UpdatePreferencesRequest(3, 31, StartSection.DASHBOARD, null),
                "A margem do aviso de frequência deve ser de no máximo 30 pontos."
        );
    }

    @Test
    void requiresTheStartSection() {
        assertRejects(new UpdatePreferencesRequest(3, 10, null, null), "Escolha a tela inicial.");
    }

    @Test
    void rejectsANegativeGradeGoal() {
        assertRejects(
                new UpdatePreferencesRequest(3, 10, StartSection.DASHBOARD, new BigDecimal("-0.1")),
                "A meta de média não pode ser negativa."
        );
    }

    @Test
    void rejectsAGradeGoalAboveTen() {
        assertRejects(
                new UpdatePreferencesRequest(3, 10, StartSection.DASHBOARD, new BigDecimal("10.1")),
                "A meta de média deve ser de no máximo 10."
        );
    }

    @Test
    void rejectsAGradeGoalWithTwoDecimalPlaces() {
        assertRejects(
                new UpdatePreferencesRequest(3, 10, StartSection.DASHBOARD, new BigDecimal("8.55")),
                "Use no máximo uma casa decimal na meta de média."
        );
    }

    private void assertRejects(Object request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
