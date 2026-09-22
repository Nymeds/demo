package studdy.example.demo.avatar.dto;

import studdy.example.demo.avatar.UserAvatar;

import java.time.Instant;

public record AvatarResponse(Instant updatedAt) {

    public static AvatarResponse from(UserAvatar avatar) {
        return new AvatarResponse(avatar.getUpdatedAt());
    }
}
