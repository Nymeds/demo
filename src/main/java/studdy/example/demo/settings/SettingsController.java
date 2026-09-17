package studdy.example.demo.settings;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.settings.dto.ChangePasswordRequest;
import studdy.example.demo.settings.dto.DeleteAccountRequest;
import studdy.example.demo.settings.dto.PreferencesResponse;
import studdy.example.demo.settings.dto.ProfileResponse;
import studdy.example.demo.settings.dto.UpdatePreferencesRequest;
import studdy.example.demo.settings.dto.UpdateProfileRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final ProfileSettingsService profileSettingsService;
    private final UserPreferencesService userPreferencesService;
    private final AccountDeletionService accountDeletionService;

    public SettingsController(
            ProfileSettingsService profileSettingsService,
            UserPreferencesService userPreferencesService,
            AccountDeletionService accountDeletionService
    ) {
        this.profileSettingsService = profileSettingsService;
        this.userPreferencesService = userPreferencesService;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal UUID userId) {
        return profileSettingsService.find(userId);
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileSettingsService.update(userId, request);
    }

    @PutMapping("/password")
    public AuthResponse changePassword(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return profileSettingsService.changePassword(userId, request);
    }

    @GetMapping("/preferences")
    public PreferencesResponse preferences(@AuthenticationPrincipal UUID userId) {
        return userPreferencesService.find(userId);
    }

    @PutMapping("/preferences")
    public PreferencesResponse updatePreferences(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        return userPreferencesService.update(userId, request);
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        accountDeletionService.delete(userId, request);
    }
}
