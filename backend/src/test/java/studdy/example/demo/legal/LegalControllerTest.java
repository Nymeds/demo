package studdy.example.demo.legal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LegalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheCurrentLegalVersionsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/legal/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsVersion").value("2026-09-29"))
                .andExpect(jsonPath("$.privacyVersion").value("2026-09-29"));
    }

    @Test
    void ignoresAnInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/legal/versions")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsVersion").value("2026-09-29"));
    }

    @Test
    void doesNotAcceptWritesOnTheLegalEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/legal/versions"))
                .andExpect(status().isUnauthorized());
    }
}
