package studdy.example.demo.user;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

// Decodifica e recodifica a foto como JPEG: remove EXIF/GPS e qualquer payload escondido,
// limita a 512x512 (só reduz) e recusa imagens com dimensões que virariam bomba de descompressão.
final class ProfilePhotoProcessor {

    static final String CONTENT_TYPE = "image/jpeg";
    static final int MAX_SIDE = 512;
    static final long MAX_PIXELS = 16_000_000L;
    // Decodifica no máximo ~2x o tamanho final: a redução fina fica para o redimensionamento.
    private static final int DECODE_TARGET_SIDE = MAX_SIDE * 2;
    private static final float JPEG_QUALITY = 0.85f;

    private ProfilePhotoProcessor() {
    }

    // Vazio quando a imagem não pode ser decodificada ou excede o limite de pixels.
    static Optional<byte[]> process(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width * height > MAX_PIXELS) {
                    return Optional.empty();
                }
                // Subamostragem na leitura: uma foto 4000x4000 é decodificada como ~1000x1000, sem
                // alocar a imagem inteira na memória.
                ImageReadParam param = reader.getDefaultReadParam();
                int step = subsamplingFor(width, height);
                param.setSourceSubsampling(step, step, 0, 0);
                BufferedImage decoded = reader.read(0, param);
                if (decoded == null) {
                    return Optional.empty();
                }
                return Optional.of(encodeJpeg(toRgb(decoded)));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    static int subsamplingFor(long width, long height) {
        return (int) Math.max(1, Math.max(width, height) / DECODE_TARGET_SIDE);
    }

    private static BufferedImage toRgb(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(out)) {
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return out.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
