package studdy.example.demo.user;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilePhotoProcessorTest {

    private static BufferedImage decode(byte[] bytes) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void stripsExifFromJpeg() throws Exception {
        byte[] input = PhotoFixtures.withExif(PhotoFixtures.jpeg(100, 80));
        assertTrue(PhotoFixtures.containsApp1(input));

        byte[] output = ProfilePhotoProcessor.process(input).orElseThrow();

        assertFalse(PhotoFixtures.containsApp1(output));
        assertFalse(new String(output, StandardCharsets.ISO_8859_1).contains("GPS-SECRET"));
        assertEquals(100, decode(output).getWidth());
    }

    @Test
    void convertsPngWithAlphaToJpeg() throws Exception {
        byte[] png = PhotoFixtures.image("png", BufferedImage.TYPE_INT_ARGB, 64, 64);

        byte[] output = ProfilePhotoProcessor.process(png).orElseThrow();

        assertEquals((byte) 0xFF, output[0]);
        assertEquals((byte) 0xD8, output[1]);
        assertNotNull(decode(output));
    }

    @Test
    void downscalesKeepingAspectRatio() throws Exception {
        BufferedImage result = decode(ProfilePhotoProcessor.process(PhotoFixtures.png(2000, 1000)).orElseThrow());

        assertEquals(512, result.getWidth());
        assertEquals(256, result.getHeight());
    }

    @Test
    void neverUpscales() throws Exception {
        BufferedImage result = decode(ProfilePhotoProcessor.process(PhotoFixtures.png(50, 30)).orElseThrow());

        assertEquals(50, result.getWidth());
        assertEquals(30, result.getHeight());
    }

    @Test
    void decodesLargeDimensionsWithSubsampling() throws Exception {
        byte[] large = PhotoFixtures.image("png", BufferedImage.TYPE_BYTE_GRAY, 4000, 3000);

        BufferedImage result = decode(ProfilePhotoProcessor.process(large).orElseThrow());

        assertEquals(512, result.getWidth());
        assertEquals(384, result.getHeight());
    }

    @Test
    void subsamplingKeepsTheDecodedImageNearTwiceTheFinalSize() {
        assertEquals(1, ProfilePhotoProcessor.subsamplingFor(50, 30));
        assertEquals(1, ProfilePhotoProcessor.subsamplingFor(2000, 1000));
        assertEquals(3, ProfilePhotoProcessor.subsamplingFor(4000, 3000));
        assertEquals(3, ProfilePhotoProcessor.subsamplingFor(3000, 4000));
    }

    @Test
    void rejectsMoreThanSixteenMegapixels() {
        byte[] tooBig = PhotoFixtures.image("png", BufferedImage.TYPE_BYTE_BINARY, 4001, 4000);

        assertTrue(ProfilePhotoProcessor.process(tooBig).isEmpty());
    }

    @Test
    void rejectsHugeDimensions() {
        byte[] bomb = PhotoFixtures.image("png", BufferedImage.TYPE_BYTE_BINARY, 7000, 7000);

        assertTrue(ProfilePhotoProcessor.process(bomb).isEmpty());
    }

    @Test
    void rejectsCorruptData() {
        byte[] corrupt = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        byte[] truncatedJpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};

        assertTrue(ProfilePhotoProcessor.process(corrupt).isEmpty());
        assertTrue(ProfilePhotoProcessor.process(truncatedJpeg).isEmpty());
    }
}
