package studdy.example.demo.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthLoginRateLimitTest {

    private static final String PASSWORD = "Senha@1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResultActions login(String email, String password, String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions register(String email, String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Estudante\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                        + "\",\"acceptedTerms\":true,\"termsVersion\":\"2026-09-29\"}"));
    }

    @Test
    void blocksAfterFiveFailuresWithProblemDetailAndRetryAfter() throws Exception {
        userRepository.save(new AppUser("Ana", "rl-ana@example.com", passwordEncoder.encode(PASSWORD)));

        for (int i = 0; i < 5; i++) {
            login("RL-Ana@example.com", "errada-000", "10.1.0.1").andExpect(status().isUnauthorized());
        }

        // Bloqueado mesmo com a senha correta a partir do mesmo IP.
        login("rl-ana@example.com", PASSWORD, "10.1.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Muitas tentativas"))
                .andExpect(jsonPath("$.detail")
                        .value("Muitas tentativas de entrada. Tente novamente em 15 minutos."));

        // O atacante nesse IP não tranca a dona da conta em outro IP.
        login("rl-ana@example.com", PASSWORD, "10.1.9.9").andExpect(status().isOk());
    }

    @Test
    void unknownEmailIsCountedAndAnswersLikeAWrongPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("rl-fantasma@example.com", "qualquer-000", "10.2.0.1")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
        }
        login("rl-fantasma@example.com", "qualquer-000", "10.2.0.1").andExpect(status().isTooManyRequests());
    }

    @Test
    void passwordAboveBcryptLimitIsInvalidCredentialsAndCountsAsFailure() throws Exception {
        userRepository.save(new AppUser("Cai", "rl-cai@example.com", passwordEncoder.encode(PASSWORD)));

        // 40 caracteres acentuados = 80 bytes em UTF-8.
        login("rl-cai@example.com", "á".repeat(40), "10.4.0.1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
        // Emojis: 4 bytes cada.
        login("rl-cai@example.com", "😀".repeat(19), "10.4.0.1").andExpect(status().isUnauthorized());
        for (int i = 0; i < 3; i++) {
            login("rl-cai@example.com", "a".repeat(73), "10.4.0.1").andExpect(status().isUnauthorized());
        }
        login("rl-cai@example.com", PASSWORD, "10.4.0.1").andExpect(status().isTooManyRequests());
    }

    @Test
    void successfulLoginResetsTheCounter() throws Exception {
        userRepository.save(new AppUser("Bia", "rl-bia@example.com", passwordEncoder.encode(PASSWORD)));

        for (int i = 0; i < 4; i++) {
            login("rl-bia@example.com", "errada-000", "10.3.0.1").andExpect(status().isUnauthorized());
        }
        login("rl-bia@example.com", PASSWORD, "10.3.0.1").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login("rl-bia@example.com", "errada-000", "10.3.0.1").andExpect(status().isUnauthorized());
        }
        login("rl-bia@example.com", PASSWORD, "10.3.0.1").andExpect(status().isOk());
    }

    @Test
    void registerIsLimitedPerIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            register("rl-cadastro-" + i + "@example.com", "10.5.0.1").andExpect(status().isCreated());
        }

        register("rl-cadastro-extra@example.com", "10.5.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value(
                        "Muitos cadastros feitos a partir desta rede. Tente novamente em 60 minutos."));

        register("rl-cadastro-outro@example.com", "10.5.0.2").andExpect(status().isCreated());
    }

    @Test
    void registerRejectsAPasswordAboveBcryptBytes() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(request -> {
                            request.setRemoteAddr("10.6.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Estudante\",\"email\":\"rl-bytes@example.com\",\"password\":\""
                                + "ç".repeat(40) + "\",\"acceptedTerms\":true,\"termsVersion\":\"2026-09-29\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(
                        "A senha é longa demais. Letras acentuadas e símbolos ocupam mais espaço; use menos caracteres."));
    }
}
