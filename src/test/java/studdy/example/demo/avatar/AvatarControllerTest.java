package studdy.example.demo.avatar;

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
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvatarControllerTest {

    private static final String AVATAR_URL = "/api/v1/settings/avatar";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAvatarRepository avatarRepository;

    @Autowired
    private JwtService jwtService;

    private AppUser user;
    private String token;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser("Estudante", "foto@example.com", "hash"));
        token = jwtService.generateToken(user.getId());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(AVATAR_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answersNoContentWhileTheUserHasNoPhoto() throws Exception {
        mockMvc.perform(get(AVATAR_URL).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void savesThePhotoAndServesItBackAsAPrivateJpeg() throws Exception {
        mockMvc.perform(multipart(AVATAR_URL).file(pngFile()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").exists());

        mockMvc.perform(get(AVATAR_URL).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("Cache-Control", containsString("private")));
    }

    @Test
    void replacesThePreviousPhotoInsteadOfKeepingTwo() throws Exception {
        mockMvc.perform(multipart(AVATAR_URL).file(pngFile()).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(multipart(AVATAR_URL).file(pngFile()).header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        long photosOfTheUser = avatarRepository.findAll().stream()
                .filter(avatar -> avatar.getUser().getId().equals(user.getId()))
                .count();

        assertEquals(1L, photosOfTheUser);
    }

    @Test
    void rejectsAFileThatIsNotAnImage() throws Exception {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file",
                "foto.png",
                "image/png",
                "isto não é uma imagem".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart(AVATAR_URL).file(fakeImage).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Formato não suportado. Envie uma imagem PNG ou JPG."));
    }

    @Test
    void explainsWhenNoFileWasSent() throws Exception {
        mockMvc.perform(multipart(AVATAR_URL).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Escolha uma imagem para enviar."));
    }

    @Test
    void neverShowsOneUsersPhotoToAnother() throws Exception {
        AppUser otherUser = userRepository.save(new AppUser("Outra pessoa", "outra-foto@example.com", "hash"));

        mockMvc.perform(multipart(AVATAR_URL).file(pngFile()).header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get(AVATAR_URL).header("Authorization", bearer(jwtService.generateToken(otherUser.getId()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void removesThePhoto() throws Exception {
        mockMvc.perform(multipart(AVATAR_URL).file(pngFile()).header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(delete(AVATAR_URL).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(AVATAR_URL).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    private static MockMultipartFile pngFile() throws IOException {
        BufferedImage image = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setColor(new Color(104, 50, 223));
            graphics.fillRect(0, 0, 320, 320);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        return new MockMultipartFile("file", "foto.png", "image/png", output.toByteArray());
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
