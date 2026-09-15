package studdy.example.demo.avatar;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarImageProcessorTest {

    private final AvatarImageProcessor processor = new AvatarImageProcessor();

    @Test
    void turnsARectangularPngIntoASquareJpegOfTheAvatarSize() throws IOException {
        byte[] avatar = processor.toAvatarJpeg(image("png", 640, 480, BufferedImage.TYPE_INT_ARGB));

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(avatar));

        assertTrue(isJpeg(avatar), "a foto salva deve ser JPEG");
        assertEquals(AvatarImageProcessor.OUTPUT_SIZE, decoded.getWidth());
        assertEquals(AvatarImageProcessor.OUTPUT_SIZE, decoded.getHeight());
    }

    @Test
    void acceptsAJpeg() throws IOException {
        byte[] avatar = processor.toAvatarJpeg(image("jpeg", 300, 300, BufferedImage.TYPE_INT_RGB));

        assertTrue(isJpeg(avatar));
    }

    @Test
    void rejectsAFileThatIsNotAnImage() {
        assertStatus(HttpStatus.BAD_REQUEST, "<svg onload=\"alert(1)\"></svg>".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsImageFormatsOutsidePngAndJpeg() throws IOException {
        assertStatus(HttpStatus.BAD_REQUEST, image("gif", 64, 64, BufferedImage.TYPE_INT_RGB));
    }

    @Test
    void rejectsAnEmptyUpload() {
        assertStatus(HttpStatus.BAD_REQUEST, new byte[0]);
    }

    @Test
    void rejectsAnUploadAboveTwoMegabytes() {
        assertStatus(HttpStatus.CONTENT_TOO_LARGE, new byte[AvatarImageProcessor.MAX_UPLOAD_BYTES + 1]);
    }

    @Test
    void rejectsHugeDimensionsBeforeDecodingTheImage() throws IOException {
        int tooWide = AvatarImageProcessor.MAX_SOURCE_DIMENSION + 1;

        assertStatus(HttpStatus.BAD_REQUEST, image("png", tooWide, 1, BufferedImage.TYPE_INT_RGB));
    }

    @Test
    void rejectsATruncatedImage() throws IOException {
        byte[] png = image("png", 200, 200, BufferedImage.TYPE_INT_RGB);

        assertStatus(HttpStatus.BAD_REQUEST, Arrays.copyOf(png, png.length / 2));
    }

    private void assertStatus(HttpStatus expected, byte[] upload) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> processor.toAvatarJpeg(upload));

        assertEquals(expected, error.getStatusCode());
    }

    private static byte[] image(String format, int width, int height, int type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();

        try {
            // Um degradê simples, para a imagem não comprimir para quase nada.
            for (int x = 0; x < width; x += 8) {
                graphics.setColor(new Color((x * 7) % 255, 90, 200));
                graphics.fillRect(x, 0, 8, height);
            }
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output), "o JDK precisa saber gravar " + format);

        return output.toByteArray();
    }

    private static boolean isJpeg(byte[] content) {
        return content.length > 2 && content[0] == (byte) 0xFF && content[1] == (byte) 0xD8;
    }
}
