package studdy.example.demo.user;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

final class PhotoFixtures {

    private PhotoFixtures() {
    }

    static byte[] image(String format, int type, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(type == BufferedImage.TYPE_INT_ARGB ? new Color(200, 30, 30, 90) : Color.BLUE);
        graphics.fillRect(0, 0, width / 2, height);
        graphics.dispose();
        return write(image, format);
    }

    static byte[] png(int width, int height) {
        return image("png", BufferedImage.TYPE_INT_RGB, width, height);
    }

    static byte[] jpeg(int width, int height) {
        return image("jpg", BufferedImage.TYPE_INT_RGB, width, height);
    }

    static byte[] write(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    // Insere um segmento APP1 (EXIF) logo depois do SOI.
    static byte[] withExif(byte[] jpeg) {
        byte[] payload = "Exif\0\0GPS-SECRET".getBytes(StandardCharsets.ISO_8859_1);
        int length = payload.length + 2;
        byte[] out = new byte[jpeg.length + payload.length + 4];
        out[0] = jpeg[0];
        out[1] = jpeg[1];
        out[2] = (byte) 0xFF;
        out[3] = (byte) 0xE1;
        out[4] = (byte) (length >> 8);
        out[5] = (byte) length;
        System.arraycopy(payload, 0, out, 6, payload.length);
        System.arraycopy(jpeg, 2, out, 6 + payload.length, jpeg.length - 2);
        return out;
    }

    static boolean containsApp1(byte[] jpeg) {
        for (int i = 0; i < jpeg.length - 1; i++) {
            if (jpeg[i] == (byte) 0xFF && jpeg[i + 1] == (byte) 0xE1) {
                return true;
            }
        }
        return false;
    }
}
