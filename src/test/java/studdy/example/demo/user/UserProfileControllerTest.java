package studdy.example.demo.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserProfileControllerTest {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String authorization;

    @BeforeEach
    void setUp() {
        AppUser user = userRepository.save(new AppUser(
                "Gabriel Silva",
                "perfil-controller@example.com",
                "hash"
        ));
        authorization = "Bearer " + jwtService.generateToken(user.getId());
    }

    @Test
    void requiresAuthenticationToReadTheProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token ausente, inválido ou expirado."));
    }

    @Test
    void readsAndUpdatesTheAuthenticatedProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gabriel Silva"))
                .andExpect(jsonPath("$.hasProfilePhoto").value(false));

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Gabriel Souza",
                                  "email": "GABRIEL.SOUZA@EXAMPLE.COM",
                                  "username": "GabrielSouza",
                                  "phone": "(62) 99999-9999",
                                  "birthDate": "2005-03-18",
                                  "gender": "PREFER_NOT_TO_SAY",
                                  "location": "Goiânia - GO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gabriel Souza"))
                .andExpect(jsonPath("$.email").value("gabriel.souza@example.com"))
                .andExpect(jsonPath("$.username").value("gabrielsouza"));
    }

    @Test
    void uploadsAndServesTheAuthenticatedUsersPhoto() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "perfil.png", "image/png", PNG);

        mockMvc.perform(multipart("/api/v1/users/me/profile-photo")
                        .file(file)
                        .header("Authorization", authorization)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProfilePhoto").value(true))
                .andExpect(jsonPath("$.profilePhotoUrl").value("/api/v1/users/me/profile-photo"));

        mockMvc.perform(get("/api/v1/users/me/profile-photo")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        mockMvc.perform(delete("/api/v1/users/me/profile-photo")
                        .header("Authorization", authorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/profile-photo")
                        .header("Authorization", authorization))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsReadableValidationErrors() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "email-inválido",
                                  "username": "nome inválido"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.name").value("O nome é obrigatório."))
                .andExpect(jsonPath("$.errors.email").value("Informe um e-mail válido."));
    }
}
