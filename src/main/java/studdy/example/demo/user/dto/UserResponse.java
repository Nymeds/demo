package studdy.example.demo.user.dto;

import studdy.example.demo.user.AppUser;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}