package studdy.example.demo.avatar;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

// Valida a imagem enviada e gera a foto de perfil padronizada: JPEG quadrado de 256 × 256.
// A foto é sempre decodificada e gravada de novo, o que descarta metadados (como a localização
// GPS da câmera) e qualquer conteúdo escondido no arquivo original.
@Component
public class AvatarImageProcessor {

    public static final int OUTPUT_SIZE = 256;
    public static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    // O recorte da tela envia 512 × 512; 2048 dá folga para outros clientes sem abrir espaço
    // para imagens que ocupem dezenas de megabytes de memória ao serem decodificadas.
    static final int MAX_SOURCE_DIMENSION = 2048;
    static final String EMPTY_UPLOAD_MESSAGE = "Escolha uma imagem para enviar.";
    static final String TOO_LARGE_MESSAGE = "A imagem deve ter no máximo 2 MB.";

    private static final Set<String> ACCEPTED_FORMATS = Set.of("png", "jpeg");
    private static final float JPEG_QUALITY = 0.9f;

    static {
        // Fotos pequenas: o ImageIO trabalha em memória em vez de criar arquivos temporários.
        ImageIO.setUseCache(false);
    }

    public byte[] toAvatarJpeg(byte[] upload) {
        if (upload == null || upload.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, EMPTY_UPLOAD_MESSAGE);
        }

        if (upload.length > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, TOO_LARGE_MESSAGE);
        }

        return encodeJpeg(cropToAvatarSquare(decode(upload)));
    }

    private BufferedImage decode(byte[] upload) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);

            if (readers == null || !readers.hasNext()) {
                throw unsupportedFormat();
            }

            ImageReader reader = readers.next();

            try {
                // O formato vem do conteúdo do arquivo, não do nome nem do tipo informado pelo navegador.
                if (!ACCEPTED_FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw unsupportedFormat();
                }

                reader.setInput(input, true, true);

                // O tamanho é conferido antes de decodificar, para uma imagem gigante não esgotar a memória.
                if (reader.getWidth(0) > MAX_SOURCE_DIMENSION || reader.getHeight(0) > MAX_SOURCE_DIMENSION) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "A imagem deve ter no máximo 2048 × 2048 pixels."
                    );
                }

                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não foi possível ler a imagem. Envie um arquivo PNG ou JPG válido."
            );
        }
    }

    private BufferedImage cropToAvatarSquare(BufferedImage source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int left = (source.getWidth() - side) / 2;
        int top = (source.getHeight() - side) / 2;

        BufferedImage avatar = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = avatar.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // JPEG não tem transparência: áreas transparentes de um PNG ficam brancas.
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
            graphics.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, left, top, left + side, top + side, null);
        } finally {
            graphics.dispose();
        }

        return avatar;
    }

    private byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam parameters = writer.getDefaultWriteParam();
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parameters.setCompressionQuality(JPEG_QUALITY);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar a foto de perfil.", exception);
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }

    private ResponseStatusException unsupportedFormat() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato não suportado. Envie uma imagem PNG ou JPG.");
    }
}
