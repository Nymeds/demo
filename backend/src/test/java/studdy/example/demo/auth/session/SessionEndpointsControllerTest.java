package studdy.example.demo.auth.session;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Limite baixo de /refresh e /logout e cookie Secure ligado, para exercitar esses caminhos do controller.
@SpringBootTest(properties = {"app.rate-limit.token.max-per-ip=2", "app.auth.cookie-secure=true"})
@AutoConfigureMockMvc
class SessionEndpointsControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired BrowserSessionService service;
    private AppUser user;

    @BeforeEach void setUp() {
        user = users.save(new AppUser("Estudante", UUID.randomUUID() + "@example.com", encoder.encode("senha-atual-123")));
    }

    @AfterEach void cleanUp() { users.deleteById(user.getId()); }

    private Cookie login() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"senha-atual-123\",\"rememberMe\":true}"))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie(SessionCookies.NAME);
    }

    private ResultActions session(String action, Cookie cookie, String address, boolean withHeader) throws Exception {
        var request = post("/api/v1/auth/" + action).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}")
                .with(r -> { r.setRemoteAddr(address); return r; });
        return mvc.perform(withHeader ? request.header("X-Session-Request", "1") : request);
    }

    @Test void sessionCookieHasSecureFlagWhenConfigured() throws Exception {
        var cookie = login();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test void refreshOverTheLimitAnswers429AndKeepsTheSession() throws Exception {
        var cookie = login();
        String address = "198.51.100.1";
        session("refresh", cookie, address, true).andExpect(status().isOk());
        session("refresh", cookie, address, true).andExpect(status().isOk());
        session("refresh", cookie, address, true).andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        // O 429 não revoga nem apaga a sessão: de outra rede o mesmo cookie continua valendo.
        session("refresh", cookie, "203.0.113.10", true).andExpect(status().isOk());
    }

    @Test void logoutOverTheLimitAnswers429AndKeepsTheSession() throws Exception {
        var cookie = login();
        String address = "192.0.2.1";
        session("refresh", cookie, address, true).andExpect(status().isOk());
        session("refresh", cookie, address, true).andExpect(status().isOk());
        session("logout", cookie, address, true).andExpect(status().isTooManyRequests())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        assertThat(service.refresh(cookie.getValue()).rememberMe()).isTrue();
    }

    @Test void refreshAndLogoutWithoutSessionHeaderAnswer403AndKeepTheSession() throws Exception {
        var cookie = login();
        session("refresh", cookie, "203.0.113.20", false).andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        session("logout", cookie, "203.0.113.21", false).andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        assertThat(service.refresh(cookie.getValue()).rememberMe()).isTrue();
    }
}
