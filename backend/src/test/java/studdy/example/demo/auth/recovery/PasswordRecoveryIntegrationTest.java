package studdy.example.demo.auth.recovery;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.auth.AuthService;
import studdy.example.demo.auth.dto.LoginRequest;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordRecoveryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordRecoveryService service;
    @Autowired PasswordRecoveryRepository recoveries;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired AuthService auth;
    @MockitoBean RecoveryMailSender mail;
    @MockitoBean Clock clock;
    private AppUser user;
    private Instant now;
    private String address;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        when(clock.instant()).thenReturn(now);
        address = UUID.randomUUID().toString();
        user = users.save(new AppUser("Estudante", UUID.randomUUID() + "@example.com", encoder.encode("senha-antiga-123")));
    }

    @AfterEach
    void cleanUp() {
        users.deleteById(user.getId());
    }

    private String requestCode() {
        service.requestCode(user.getEmail());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mail, atLeastOnce()).sendAfterCommit(eq(user.getEmail()), captor.capture());
        return captor.getValue();
    }

    @Test
    void completesPublicHttpFlowRevokesSessionsAndAllowsImmediateLogin() throws Exception {
        String oldToken = jwt.generateToken(user.getId());
        String code = requestCode();
        String json = mvc.perform(post("/api/v1/auth/password-recovery/verify")
                        .with(request -> { request.setRemoteAddr(address); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.expiresIn").value(300)).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.resetToken");
        mvc.perform(post("/api/v1/auth/password-recovery/reset")
                        .with(request -> { request.setRemoteAddr(address); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"resetToken\":\"" + token + "\",\"newPassword\":\"senha-nova-123\"}"))
                .andExpect(status().isOk());
        assertThat(encoder.matches("senha-nova-123", users.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
        assertThatThrownBy(() -> auth.login(new LoginRequest(user.getEmail(), "senha-antiga-123"), address))
                .isInstanceOf(ResponseStatusException.class);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldToken)).andExpect(status().isUnauthorized());
        var login = auth.login(new LoginRequest(user.getEmail(), "senha-nova-123"), address);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + login.accessToken())).andExpect(status().isOk());
        assertThatThrownBy(() -> service.resetPassword(user.getEmail(), token, "outra-senha-123"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void returnsSameResponseForKnownUnknownAndCooldownEmails() throws Exception {
        String known = requestEmail(user.getEmail());
        assertThat(requestEmail("unknown@example.com")).isEqualTo(known);
        assertThat(requestEmail(user.getEmail().toUpperCase())).isEqualTo(known);
        verify(mail, timeout(5000).times(1)).sendAfterCommit(eq(user.getEmail()), anyString());
        assertThat(known).doesNotContain("resetToken", "code");
    }

    private String requestEmail(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-recovery/request")
                        .with(request -> { request.setRemoteAddr(address); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void storesOnlyHashesAndConsumesCodeOnceVerified() {
        String code = requestCode();
        assertThat(recoveries.findById(user.getId()).orElseThrow().getCodeHash()).hasSize(64).isNotEqualTo(code);
        String token = service.verifyCode(user.getEmail(), code);
        var recovery = recoveries.findById(user.getId()).orElseThrow();
        assertThat(recovery.getCodeHash()).isNull();
        assertThat(recovery.getTokenHash()).hasSize(64).isNotEqualTo(token);
        assertThatThrownBy(() -> service.verifyCode(user.getEmail(), code)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void persistsFailedAttemptsAcrossSeparateHttpTransactions() throws Exception {
        String code = requestCode();
        String incorrect = "000000".equals(code) ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/password-recovery/verify")
                            .with(request -> { request.setRemoteAddr(address); return request; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + user.getEmail() + "\",\"code\":\"" + incorrect + "\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        }
        assertThat(recoveries.findById(user.getId()).orElseThrow().getFailedAttempts()).isEqualTo(5);
        assertThatThrownBy(() -> service.verifyCode(user.getEmail(), code)).isInstanceOf(ResponseStatusException.class);
        assertThat(encoder.matches("senha-antiga-123", users.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void rejectsExpiredCodeAtExactDeadline() {
        String code = requestCode();
        when(clock.instant()).thenReturn(now.plusSeconds(600));
        assertThatThrownBy(() -> service.verifyCode(user.getEmail(), code)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsExpiredAuthorizationAndTokenFromAnotherEmail() {
        String token = service.verifyCode(user.getEmail(), requestCode());
        assertThatThrownBy(() -> service.resetPassword("other@example.com", token, "senha-nova-123"))
                .isInstanceOf(ResponseStatusException.class);
        when(clock.instant()).thenReturn(now.plusSeconds(300));
        assertThatThrownBy(() -> service.resetPassword(user.getEmail(), token, "senha-nova-123"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resendRevokesPreviousAuthorizationAndRespectsHourlyLimit() {
        String token = service.verifyCode(user.getEmail(), requestCode());
        when(clock.instant()).thenReturn(now.plusSeconds(60));
        String newCode = requestCode();
        assertThatThrownBy(() -> service.resetPassword(user.getEmail(), token, "senha-nova-123"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.verifyCode(user.getEmail(), newCode)).hasSize(43);
        for (int i = 2; i <= 5; i++) {
            when(clock.instant()).thenReturn(now.plusSeconds(i * 60L));
            service.requestCode(user.getEmail());
        }
        verify(mail, times(5)).sendAfterCommit(eq(user.getEmail()), anyString());
        when(clock.instant()).thenReturn(now.plusSeconds(3600));
        service.requestCode(user.getEmail());
        verify(mail, times(6)).sendAfterCommit(eq(user.getEmail()), anyString());
    }

    @Test
    void changingCredentialsOrEmailInvalidatesRecovery() {
        String code = requestCode();
        var account = users.findById(user.getId()).orElseThrow();
        account.changePasswordHash(encoder.encode("outra-senha-123"));
        users.saveAndFlush(account);
        assertThatThrownBy(() -> service.verifyCode(user.getEmail(), code)).isInstanceOf(ResponseStatusException.class);
        when(clock.instant()).thenReturn(now.plusSeconds(60));
        String token = service.verifyCode(user.getEmail(), requestCode());
        account = users.findById(user.getId()).orElseThrow();
        account.updateProfile(account.getName(), "changed-" + user.getEmail(), account.getUsername(),
                account.getPhone(), account.getBirthDate(), account.getGender(), account.getLocation());
        users.saveAndFlush(account);
        String changedEmail = account.getEmail();
        assertThatThrownBy(() -> service.resetPassword(changedEmail, token, "senha-nova-123"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void limitsRequestsByPeerAddressWithoutTrustingForwardedHeaders() throws Exception {
        for (int i = 0; i < 30; i++) requestEmail("unknown@example.com");
        mvc.perform(post("/api/v1/auth/password-recovery/request")
                        .with(request -> { request.setRemoteAddr(address); return request; })
                        .header("X-Forwarded-For", "203.0.113.1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "3600"));
    }

    @Test
    void validatesPublicInputsAndBcryptByteLimit() throws Exception {
        mvc.perform(post("/api/v1/auth/password-recovery/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
        String token = service.verifyCode(user.getEmail(), requestCode());
        assertThatThrownBy(() -> service.resetPassword(user.getEmail(), token, "é".repeat(40)))
                .isInstanceOf(ResponseStatusException.class);
        service.resetPassword(user.getEmail(), token, "senha-nova-123");
    }

    @Test
    void concurrentResetConsumesAuthorizationOnlyOnce() throws Exception {
        String token = service.verifyCode(user.getEmail(), requestCode());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var operation = (java.util.concurrent.Callable<Boolean>) () -> {
                start.await();
                try {
                    service.resetPassword(user.getEmail(), token, "senha-nova-123");
                    return true;
                } catch (ResponseStatusException exception) {
                    return false;
                }
            };
            var first = executor.submit(operation);
            var second = executor.submit(operation);
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void deletingAccountCascadesRecoveryData() {
        requestCode();
        users.deleteById(user.getId());
        assertThat(recoveries.findById(user.getId())).isEmpty();
    }
}
