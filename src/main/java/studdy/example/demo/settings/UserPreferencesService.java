package studdy.example.demo.settings;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.settings.dto.PreferencesResponse;
import studdy.example.demo.settings.dto.UpdatePreferencesRequest;
import studdy.example.demo.user.UserRepository;

import java.util.UUID;

@Service
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    public UserPreferencesService(UserPreferencesRepository preferencesRepository, UserRepository userRepository) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
    }

    // Quem nunca salvou preferências recebe os valores padrão sem criar linha no banco.
    @Transactional(readOnly = true)
    public PreferencesResponse find(UUID userId) {
        return preferencesRepository.findByUser_Id(userId)
                .map(PreferencesResponse::from)
                .orElseGet(PreferencesResponse::defaults);
    }

    @Transactional
    public PreferencesResponse update(UUID userId, UpdatePreferencesRequest request) {
        // O usuário vem do token já validado pelo filtro, então a referência dispensa um select.
        UserPreferences preferences = preferencesRepository.findByUser_Id(userId)
                .orElseGet(() -> new UserPreferences(userRepository.getReferenceById(userId)));

        preferences.update(
                request.deadlineAlertDays(),
                request.attendanceAlertMargin(),
                request.startSection(),
                request.gradeGoal()
        );

        try {
            return PreferencesResponse.from(preferencesRepository.saveAndFlush(preferences));
        } catch (DataIntegrityViolationException exception) {
            // Duas abas criaram as primeiras preferências ao mesmo tempo e a outra gravou antes.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Suas preferências acabaram de ser salvas em outra aba. Tente salvar novamente."
            );
        }
    }
}
