package studdy.example.demo.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import studdy.example.demo.user.dto.UpdateProfileRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateProfileRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsACompleteValidProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Gabriel Silva",
                "gabriel@example.com",
                "gabrielsilva",
                "(62) 99999-9999",
                LocalDate.of(2005, 3, 18),
                Gender.PREFER_NOT_TO_SAY,
                "Goiânia - GO",
                null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsEmptyOptionalFields() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Gabriel Silva",
                "gabriel@example.com",
                "",
                "",
                null,
                null,
                "",
                null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankName() {
        assertRejects(validRequest("   ", "gabriel@example.com", "gabrielsilva", "(62) 99999-9999", null),
                "O nome é obrigatório.");
    }

    @Test
    void rejectsInvalidEmail() {
        assertRejects(validRequest("Gabriel", "email-inválido", "gabrielsilva", "(62) 99999-9999", null),
                "Informe um e-mail válido.");
    }

    @Test
    void rejectsInvalidUsername() {
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "ga brie!", "(62) 99999-9999", null),
                "O nome de usuário deve ter entre 3 e 30 caracteres e usar apenas letras, números, ponto ou sublinhado.");
    }

    @Test
    void rejectsInvalidPhone() {
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "gabrielsilva", "123", null),
                "Informe um celular válido com DDD: (DD) 9XXXX-XXXX, começando com 9 após o DDD.");
    }

    @Test
    void rejectsPhoneNotStartingWithNineAfterAreaCode() {
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "gabrielsilva", "(11) 81234-5678", null),
                "Informe um celular válido com DDD: (DD) 9XXXX-XXXX, começando com 9 após o DDD.");
    }

    @Test
    void rejectsIncompletePhoneAndInvalidAreaCode() {
        String message = "Informe um celular válido com DDD: (DD) 9XXXX-XXXX, começando com 9 após o DDD.";
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "gabrielsilva", "(11) 91234-567", null), message);
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "gabrielsilva", "(01) 91234-5678", null), message);
        assertRejects(validRequest("Gabriel", "gabriel@example.com", "gabrielsilva", "119123456789", null), message);
    }

    @Test
    void acceptsDigitsOnlyAndCountryCodePhones() {
        for (String phone : new String[] {"11912345678", "+55 11 91234-5678", "(11) 91234-5678"}) {
            assertTrue(validator.validate(validRequest("Gabriel", "gabriel@example.com", "gabrielsilva", phone, null))
                    .isEmpty(), phone);
        }
    }

    @Test
    void rejectsFutureBirthDate() {
        assertRejects(validRequest(
                        "Gabriel",
                        "gabriel@example.com",
                        "gabrielsilva",
                        "(62) 99999-9999",
                        LocalDate.now().plusDays(1)
                ),
                "A data de nascimento deve estar no passado.");
    }

    private UpdateProfileRequest validRequest(
            String name,
            String email,
            String username,
            String phone,
            LocalDate birthDate
    ) {
        return new UpdateProfileRequest(
                name,
                email,
                username,
                phone,
                birthDate,
                Gender.PREFER_NOT_TO_SAY,
                "Goiânia - GO",
                null
        );
    }

    private void assertRejects(UpdateProfileRequest request, String expectedMessage) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }
}
