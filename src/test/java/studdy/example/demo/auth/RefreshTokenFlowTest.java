package studdy.example.demo.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RefreshTokenFlowTest.MutableClockConfig.class)
class RefreshTokenFlowTest {

    private static final String PASSWORD = "Senha@1234";
    private static final String EXPIRED = "Sua sessão expirou. Entre novamente.";

    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-29T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class MutableClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private RefreshTokenService purge;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser("Ana", "refresh-ana@example.com", passwordEncoder.encode(PASSWORD)));
    }

    private ResultActions login(boolean rememberMe) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"refresh-ana@example.com\",\"password\":\"" + PASSWORD
                        + "\",\"rememberMe\":" + rememberMe + "}"));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private ResultActions logout(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private String field(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    @Test
    void loginReturnsRefreshTokenWithTwelveHoursByDefaultAndThirtyDaysWithRememberMe() throws Exception {
        ResultActions shortSession = login(false).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        assertEquals(clock.instant().plus(Duration.ofHours(12)),
                Instant.parse(field(shortSession, "$.refreshTokenExpiresAt")));

        ResultActions longSession = login(true).andExpect(status().isOk());
        assertEquals(clock.instant().plus(Duration.ofDays(30)),
                Instant.parse(field(longSession, "$.refreshTokenExpiresAt")));
    }

    @Test
    void loginWithoutRememberMeFieldBehavesAsFalse() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"refresh-ana@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
        assertEquals(clock.instant().plus(Duration.ofHours(12)),
                Instant.parse(field(result, "$.refreshTokenExpiresAt")));
    }

    @Test
    void storesOnlyTheHashOfTheRefreshToken() throws Exception {
        String token = field(login(false), "$.refreshToken");

        List<RefreshToken> stored = refreshTokenRepository.findAll();
        assertEquals(1, stored.size());
        assertNotEquals(token, stored.get(0).getTokenHash());
        assertEquals(RefreshTokenService.hash(token), stored.get(0).getTokenHash());
        assertEquals(43, token.length());
    }

    @Test
    void refreshRotatesTheTokenKeepingTheFamilyAndAbsoluteExpiry() throws Exception {
        ResultActions loginResult = login(true);
        String first = field(loginResult, "$.refreshToken");
        Instant expiresAt = Instant.parse(field(loginResult, "$.refreshTokenExpiresAt"));

        clock.advance(Duration.ofDays(1));
        ResultActions refreshed = refresh(first).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900));
        String second = field(refreshed, "$.refreshToken");

        assertNotEquals(first, second);
        assertEquals(expiresAt, Instant.parse(field(refreshed, "$.refreshTokenExpiresAt")));

        RefreshToken old = refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(first)).orElseThrow();
        RefreshToken next = refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(second)).orElseThrow();
        assertTrue(old.isRevoked());
        assertEquals(next.getId(), old.getReplacedBy());
        assertEquals(old.getFamilyId(), next.getFamilyId());
        assertFalse(next.isRevoked());

        // O novo access token autentica.
        String access = field(refreshed, "$.accessToken");
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() throws Exception {
        String first = field(login(false), "$.refreshToken");
        String second = field(refresh(first).andExpect(status().isOk()), "$.refreshToken");

        clock.advance(Duration.ofSeconds(11));
        refresh(first).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(EXPIRED));

        // O token legítimo mais recente também caiu.
        refresh(second).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(EXPIRED));
        assertTrue(refreshTokenRepository.findAll().stream().allMatch(RefreshToken::isRevoked));
    }

    @Test
    void reuseWithinTheGraceWindowIsRejectedWithoutRevokingTheFamily() throws Exception {
        String first = field(login(false), "$.refreshToken");
        String second = field(refresh(first).andExpect(status().isOk()), "$.refreshToken");

        // Outra aba renovou com o mesmo token segundos depois: só ela recebe 401.
        clock.advance(Duration.ofSeconds(9));
        refresh(first).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(EXPIRED));

        refresh(second).andExpect(status().isOk());
    }

    @Test
    void logoutRevokesTheWholeFamily() throws Exception {
        String first = field(login(false), "$.refreshToken");
        String second = field(refresh(first).andExpect(status().isOk()), "$.refreshToken");
        String otherSession = field(login(false), "$.refreshToken");

        // Logout com o token antigo (ex.: outra aba) também encerra a sessão rotacionada.
        logout(first).andExpect(status().isNoContent());

        refresh(second).andExpect(status().isUnauthorized());
        refresh(otherSession).andExpect(status().isOk());
    }

    @Test
    void loginRightAfterPasswordChangeProducesAWorkingAccessToken() throws Exception {
        String access = field(login(false), "$.accessToken");
        mockMvc.perform(org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders.put("/api/v1/settings/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NovaSenha@5678\"}"))
                .andExpect(status().isOk());

        // Mesmo segundo da troca: o iat não pode ficar antes de credentialsUpdatedAt.
        String fresh = field(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"refresh-ana@example.com\",\"password\":\"NovaSenha@5678\"}"))
                .andExpect(status().isOk()), "$.accessToken");
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + fresh))
                .andExpect(status().isOk());

        // A senha antiga deixou de valer.
        login(false).andExpect(status().isUnauthorized());
    }

    @Test
    void purgeRemovesTokensExpiredOrRevokedMoreThanADayAgo() throws Exception {
        String revoked = field(login(false), "$.refreshToken");
        logout(revoked);
        String active = field(login(true), "$.refreshToken");
        String expiring = field(login(false), "$.refreshToken");

        clock.advance(Duration.ofHours(12).plusDays(1).plusMinutes(1));
        int removed = purge.purgeStale();

        assertEquals(2, removed);
        assertTrue(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(active)).isPresent());
        assertTrue(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(expiring)).isEmpty());
    }

    @Test
    void reuseDoesNotAffectOtherFamilies() throws Exception {
        String sessionA = field(login(false), "$.refreshToken");
        String sessionB = field(login(false), "$.refreshToken");
        refresh(sessionA).andExpect(status().isOk());
        clock.advance(Duration.ofSeconds(11));
        refresh(sessionA).andExpect(status().isUnauthorized());

        refresh(sessionB).andExpect(status().isOk());
    }

    @Test
    void expiredTokenIsRejectedByTheClock() throws Exception {
        String token = field(login(false), "$.refreshToken");

        clock.advance(Duration.ofHours(12));

        refresh(token).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Sessão expirada"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(EXPIRED));
    }

    @Test
    void rememberMeTokenSurvivesPastTwelveHoursButNotThirtyDays() throws Exception {
        String token = field(login(true), "$.refreshToken");

        clock.advance(Duration.ofHours(13));
        String rotated = field(refresh(token).andExpect(status().isOk()), "$.refreshToken");

        clock.advance(Duration.ofDays(30));
        refresh(rotated).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownTokenIsUnauthorizedAndBlankIsBadRequest() throws Exception {
        refresh("token-que-nao-existe").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(EXPIRED));
        refresh("").andExpect(status().isBadRequest());
    }

    @Test
    void refreshAndLogoutDoNotRequireAnAccessToken() throws Exception {
        String token = field(login(false), "$.refreshToken");

        refresh(token).andExpect(status().isOk());
        logout("qualquer").andExpect(status().isNoContent());
    }

    @Test
    void logoutRevokesTheTokenAndIsIdempotent() throws Exception {
        String token = field(login(false), "$.refreshToken");

        logout(token).andExpect(status().isNoContent());
        logout(token).andExpect(status().isNoContent());
        logout("desconhecido").andExpect(status().isNoContent());

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenRejectionReturnsProblemDetailWithStatus401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Sessão expirada"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(EXPIRED));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer lixo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Sessão expirada"));
    }

    @Test
    void passwordChangeRevokesEveryRefreshTokenButIssuesAFreshOneForTheCaller() throws Exception {
        String access = field(login(true), "$.accessToken");
        String other = field(login(false), "$.refreshToken");

        ResultActions changed = mockMvc.perform(org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders.put("/api/v1/settings/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NovaSenha@5678\"}"));
        changed.andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").isNotEmpty());

        refresh(other).andExpect(status().isUnauthorized());
        String fresh = field(changed, "$.refreshToken");
        assertNotNull(fresh);
        refresh(fresh).andExpect(status().isOk());
    }

    @Test
    void emailChangeRevokesRefreshTokens() throws Exception {
        ResultActions loginResult = login(true);
        String access = field(loginResult, "$.accessToken");
        String token = field(loginResult, "$.refreshToken");

        ResultActions updated = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"refresh-ana2@example.com\",\"currentPassword\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("refresh-ana2@example.com"))
                .andExpect(jsonPath("$.session.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.session.expiresIn").value(900))
                .andExpect(jsonPath("$.session.refreshTokenExpiresAt").isNotEmpty());

        refresh(token).andExpect(status().isUnauthorized());

        // O novo par funciona: access token autentica e o refresh token renova.
        String newAccess = field(updated, "$.session.accessToken");
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").doesNotExist());
        refresh(field(updated, "$.session.refreshToken")).andExpect(status().isOk());
    }

    @Test
    void nameOnlyChangeKeepsRefreshTokens() throws Exception {
        ResultActions loginResult = login(true);
        String access = field(loginResult, "$.accessToken");
        String token = field(loginResult, "$.refreshToken");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Maria\",\"email\":\"refresh-ana@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").doesNotExist());

        refresh(token).andExpect(status().isOk());
    }
}
