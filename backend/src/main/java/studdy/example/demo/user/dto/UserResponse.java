package studdy.example.demo.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import studdy.example.demo.auth.dto.AuthResponse;
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
        Instant updatedAt,
        // Versão dos Termos aceita pela conta; null se ainda não houve aceite registrado.
        String termsAcceptedVersion,
        // Só presente quando a operação trocou as credenciais (ex.: e-mail em PUT /users/me): as
        // sessões anteriores caíram e o cliente deve passar a usar este novo access token.
        @JsonInclude(JsonInclude.Include.NON_NULL)
        AuthResponse session
) {
    public UserResponse withSession(AuthResponse newSession) {
        return new UserResponse(id, name, email, username, phone, birthDate, gender, location,
                hasProfilePhoto, profilePhotoUrl, createdAt, updatedAt, termsAcceptedVersion, newSession);
    }

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
                user.getUpdatedAt(),
                user.getTermsVersion(),
                null
        );
    }
}
