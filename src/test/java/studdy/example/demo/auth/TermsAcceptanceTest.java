package studdy.example.demo.auth;

import com.jayway.jsonpath.JsonPath;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.auth.dto.RegisterRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TermsAcceptanceTest {

    private static final String CURRENT_VERSION = "2026-09-29";
    private static final String TERMS_MESSAGE = "É necessário aceitar os Termos de Uso e a Política de Privacidade.";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Clock clock;

    private ResultActions register(String email, String acceptedTermsJson, String versionJson) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"Senha@1234\""
                        + acceptedTermsJson + versionJson + "}"));
    }

    @Test
    void registrationRecordsTheAcceptanceOnTheUser() throws Exception {
        var before = clock.instant();

        register("termos-ok@example.com", ",\"acceptedTerms\":true", ",\"termsVersion\":\"" + CURRENT_VERSION + "\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.termsAcceptedVersion").value(CURRENT_VERSION));

        AppUser user = userRepository.findByEmail("termos-ok@example.com").orElseThrow();
        assertEquals(CURRENT_VERSION, user.getTermsVersion());
        assertEquals(CURRENT_VERSION, user.getPrivacyVersion());
        assertNotNull(user.getTermsAcceptedAt());
        assertTrue(!user.getTermsAcceptedAt().isBefore(before));
    }

    @Test
    void rejectsRegistrationWithoutAcceptance() throws Exception {
        register("termos-nao@example.com", ",\"acceptedTerms\":false", ",\"termsVersion\":\"" + CURRENT_VERSION + "\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.acceptedTerms").value(TERMS_MESSAGE));

        register("termos-omitido@example.com", "", ",\"termsVersion\":\"" + CURRENT_VERSION + "\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.acceptedTerms").value(TERMS_MESSAGE));

        assertTrue(userRepository.findByEmail("termos-nao@example.com").isEmpty());
    }

    @Test
    void rejectsMissingTermsVersion() throws Exception {
        register("termos-sem-versao@example.com", ",\"acceptedTerms\":true", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.termsVersion").exists());
    }

    @Test
    void rejectsAMismatchedVersionWith400() throws Exception {
        register("termos-velho@example.com", ",\"acceptedTerms\":true", ",\"termsVersion\":\"2020-01-01\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Os Termos de Uso foram atualizados. Recarregue a página e leia a versão atual."));

        assertTrue(userRepository.findByEmail("termos-velho@example.com").isEmpty());
    }

    @Test
    void requestValidationRequiresAcceptance() {
        RegisterRequest request = new RegisterRequest("Ana", "a@example.com", "senha-segura", false, CURRENT_VERSION);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getMessage().equals(TERMS_MESSAGE)));
    }

    @Test
    void legalVersionsEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/legal/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsVersion").value(CURRENT_VERSION))
                .andExpect(jsonPath("$.privacyVersion").value(CURRENT_VERSION));
    }

    @Test
    void existingUsersWithoutAcceptanceCanStillLoginAndSeeNullVersion() throws Exception {
        userRepository.save(new AppUser("Antigo", "antigo@example.com",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Senha@1234")));

        String access = JsonPath.read(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"antigo@example.com\",\"password\":\"Senha@1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");

        var me = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsAcceptedVersion").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertNull(JsonPath.read(me, "$.termsAcceptedVersion"));
    }
}
