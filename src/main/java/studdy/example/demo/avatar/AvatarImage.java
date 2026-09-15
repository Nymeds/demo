package studdy.example.demo.avatar;

import java.time.Instant;

// Foto pronta para a resposta HTTP, copiada da entidade ainda dentro da transação.
public record AvatarImage(byte[] content, String contentType, Instant updatedAt) {
}
