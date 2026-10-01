package studdy.example.demo.auth.session;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import studdy.example.demo.auth.recovery.RecoveryMailSender;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.access-token-expiration-ms=900000")
@AutoConfigureMockMvc
class BrowserSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BrowserSessionRepository sessions;
    @Autowired BrowserSessionService service;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @MockitoBean Clock clock;
    @MockitoBean RecoveryMailSender mail;
    private AppUser user;
    private Instant now;

    @BeforeEach void setUp() {
        now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        when(clock.instant()).thenReturn(now);
        user = users.save(new AppUser("Estudante", UUID.randomUUID() + "@example.com", encoder.encode("senha-atual-123")));
    }

    @AfterEach void cleanUp() { users.deleteById(user.getId()); }

    private MvcResult login(boolean rememberMe) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"senha-atual-123\",\"rememberMe\":" + rememberMe + "}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.expiresIn").value(900)).andReturn();
    }

    private Cookie savedCookie(MvcResult result) { return result.getResponse().getCookie(SessionCookies.NAME); }
    private String accessToken(MvcResult result) throws Exception { return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken"); }

    @Test void rememberedLoginUsesPersistentHttpOnlyCookieAndStoresOnlyHash() throws Exception {
        var result = login(true);
        var cookie = savedCookie(result);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getMaxAge()).isEqualTo(30 * 86400);
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
        assertThat(cookie.getValue()).matches("[A-Za-z0-9_-]{43}");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(cookie.getValue());
        var row = sessions.findByUserIdOrderByCreatedAtDesc(user.getId()).getFirst();
        assertThat(row.getTokenHash()).matches("[0-9a-f]{64}").isNotEqualTo(cookie.getValue());
        assertThat(row.getExpiresAt()).isEqualTo(now.plus(30, ChronoUnit.DAYS));
    }

    @Test void uncheckedAndLegacyClientsReceiveBrowserCookieWithTwelveHourLimit() throws Exception {
        var result = login(false);
        assertThat(savedCookie(result).getMaxAge()).isEqualTo(-1);
        assertThat(result.getResponse().getHeader("Set-Cookie")).doesNotContain("Max-Age", "Expires=");
        assertThat(sessions.findByUserIdOrderByCreatedAtDesc(user.getId()).getFirst().getExpiresAt())
                .isEqualTo(now.plus(12, ChronoUnit.HOURS));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"senha-atual-123\"}"))
                .andExpect(status().isOk()).andExpect(cookie().maxAge(SessionCookies.NAME, -1));
    }

    @Test void refreshWorksAfterShortAccessLifetimeWithoutExtendingRememberedDeadline() throws Exception {
        var remembered = savedCookie(login(true));
        when(clock.instant()).thenReturn(now.plus(1, ChronoUnit.DAYS));
        var result = mvc.perform(post("/api/v1/auth/refresh").cookie(remembered)
                        .contentType(MediaType.APPLICATION_JSON).header("X-Session-Request", "1").content("{}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.rememberMe").value(true)).andExpect(jsonPath("$.expiresIn").value(900)).andReturn();
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken(result))).andExpect(status().isOk());
        assertThat(sessions.findByUserIdOrderByCreatedAtDesc(user.getId()).getFirst().getExpiresAt())
                .isEqualTo(now.plus(30, ChronoUnit.DAYS));
    }

    @Test void bothSessionTypesExpireExactlyAtTheirDeadlineAndClearCookie() throws Exception {
        for (boolean remembered : new boolean[]{false, true}) {
            when(clock.instant()).thenReturn(now);
            var sessionCookie = savedCookie(login(remembered));
            when(clock.instant()).thenReturn(now.plus(remembered ? 30 * 86400 : 12 * 3600, ChronoUnit.SECONDS));
            mvc.perform(post("/api/v1/auth/refresh").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON)
                            .header("X-Session-Request", "1").content("{}"))
                    .andExpect(status().isUnauthorized()).andExpect(cookie().maxAge(SessionCookies.NAME, 0))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    @Test void refreshRejectsMissingCookieAndCrossSiteRequests() throws Exception {
        var sessionCookie = savedCookie(login(true));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).header("X-Session-Request", "1").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/refresh").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Session-Request", "1").header("Sec-Fetch-Site", "cross-site").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout").cookie(sessionCookie).contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/v1/auth/logout").cookie(sessionCookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertThat(service.refresh(sessionCookie.getValue()).rememberMe()).isTrue();
    }

    @Test void logoutRevokesSavedSessionAndIsIdempotent() throws Exception {
        var remembered = savedCookie(login(true));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/auth/logout").cookie(remembered).contentType(MediaType.APPLICATION_JSON)
                            .header("X-Session-Request", "1").content("{}"))
                    .andExpect(status().isNoContent()).andExpect(cookie().maxAge(SessionCookies.NAME, 0));
        }
        assertThat(sessions.findByUserIdOrderByCreatedAtDesc(user.getId())).isEmpty();
        mvc.perform(post("/api/v1/auth/refresh").cookie(remembered).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Session-Request", "1").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void passwordChangeReplacesCurrentCookieAndRevokesOtherBrowsers() throws Exception {
        var current = login(true);
        var other = savedCookie(login(true));
        var result = mvc.perform(put("/api/v1/settings/password").cookie(savedCookie(current))
                        .header("Authorization", "Bearer " + accessToken(current)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"senha-atual-123\",\"newPassword\":\"senha-nova-123\",\"confirmPassword\":\"senha-nova-123\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(savedCookie(result).getValue()).isNotEqualTo(savedCookie(current).getValue());
        assertThat(service.refresh(savedCookie(result).getValue()).rememberMe()).isTrue();
        assertThatThrownBy(() -> service.refresh(other.getValue())).hasMessageContaining("401");
        assertThatThrownBy(() -> service.refresh(savedCookie(current).getValue())).hasMessageContaining("401");
    }

    @Test void resetPasswordAndDeleteAccountInvalidateSavedSessions() throws Exception {
        var remembered = savedCookie(login(true));
        var account = users.findById(user.getId()).orElseThrow();
        account.changePasswordHash(encoder.encode("nova-senha-123"));
        users.saveAndFlush(account);
        assertThatThrownBy(() -> service.refresh(remembered.getValue())).hasMessageContaining("401");
        users.deleteById(user.getId());
        assertThat(sessions.findByUserIdOrderByCreatedAtDesc(user.getId())).isEmpty();
    }

    @Test void repeatedLoginsAreBoundedAndReplacingCookieRevokesPreviousSession() throws Exception {
        for (int i = 0; i < 12; i++) service.create(user.getId(), true, null);
        assertThat(sessions.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(10);
        var previous = service.create(user.getId(), true, null);
        var next = service.create(user.getId(), false, previous.token());
        assertThatThrownBy(() -> service.refresh(previous.token())).hasMessageContaining("401");
        assertThat(service.refresh(next.token()).rememberMe()).isFalse();
        assertThat(sessions.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(10);
    }
}
