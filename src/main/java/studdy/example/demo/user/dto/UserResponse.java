package studdy.example.demo.user.dto;

import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String username,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String location,
        boolean hasProfilePhoto,
        String profilePhotoUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(AppUser user) {
        return from(user, false);
    }

    public static UserResponse from(AppUser user, boolean hasProfilePhoto) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getPhone(),
                user.getBirthDate(),
                user.getGender(),
                user.getLocation(),
                hasProfilePhoto,
                hasProfilePhoto ? "/api/v1/users/me/profile-photo" : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
