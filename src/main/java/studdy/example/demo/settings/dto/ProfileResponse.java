package studdy.example.demo.settings.dto;

import studdy.example.demo.user.AppUser;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String name,
        String email,
        Instant createdAt,
        Instant passwordChangedAt
) {
    public static ProfileResponse from(AppUser user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getCredentialsUpdatedAt()
        );
    }
}
