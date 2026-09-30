package studdy.example.demo.settings;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SettingsControllerTest {

    private static final String PASSWORD = "senha-atual-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private AppUser user;
    private String token;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser("Estudante", "rotas@example.com", passwordEncoder.encode(PASSWORD)));
        token = jwtService.generateToken(user.getId());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/settings/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheProfileWithoutSensitiveFields() throws Exception {
        mockMvc.perform(get("/api/v1/settings/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rotas@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void explainsWhyTheCurrentPasswordWasRejected() throws Exception {
        mockMvc.perform(put("/api/v1/settings/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "senha-errada-000", "newPassword": "nova-senha-456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A senha atual está incorreta."));
    }

    @Test
    void reportsEachInvalidPreferenceField() throws Exception {
        mockMvc.perform(put("/api/v1/settings/preferences")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deadlineAlertDays": 0, "attendanceAlertMargin": 10, "startSection": "DASHBOARD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.deadlineAlertDays").exists());
    }

    @Test
    void savesThePreferencesAndReadsThemBack() throws Exception {
        mockMvc.perform(put("/api/v1/settings/preferences")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deadlineAlertDays": 5, "attendanceAlertMargin": 15, "startSection": "ACTIVITIES"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/settings/preferences").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineAlertDays").value(5))
                .andExpect(jsonPath("$.attendanceAlertMargin").value(15))
                .andExpect(jsonPath("$.startSection").value("ACTIVITIES"));
    }

    @Test
    void revokesTokensIssuedBeforeThePasswordChange() throws Exception {
        String oldToken = tokenIssuedAt(Instant.now().minusSeconds(60));

        String body = mockMvc.perform(put("/api/v1/settings/password")
                        .header("Authorization", bearer(oldToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "senha-atual-123", "newPassword": "nova-senha-456"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String newToken = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/v1/settings/profile").header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/settings/profile").header("Authorization", bearer(newToken)))
                .andExpect(status().isOk());
    }

    @Test
    void revokesATokenIssuedMomentsBeforeThePasswordChange() throws Exception {
        // O token do setUp foi emitido agora há pouco, muito provavelmente no mesmo segundo da troca.
        mockMvc.perform(put("/api/v1/settings/password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "senha-atual-123", "newPassword": "nova-senha-456"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/settings/profile").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletesTheAccountAndRejectsItsTokenAfterwards() throws Exception {
        mockMvc.perform(delete("/api/v1/settings/account")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "senha-atual-123"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/settings/profile").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    // Monta um token com a data de emissão escolhida, algo que o JwtService não permite.
    private String tokenIssuedAt(Instant issuedAt) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
