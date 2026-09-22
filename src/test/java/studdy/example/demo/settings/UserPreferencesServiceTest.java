package studdy.example.demo.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.settings.dto.PreferencesResponse;
import studdy.example.demo.settings.dto.UpdatePreferencesRequest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserPreferencesServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferencesRepository preferencesRepository;

    @Autowired
    private UserPreferencesService userPreferencesService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser("Estudante", "preferencias@example.com", "hash"));
    }

    @Test
    void returnsTheDefaultsWithoutSavingAnything() {
        assertEquals(
                new PreferencesResponse(3, 10, StartSection.DASHBOARD, null),
                userPreferencesService.find(user.getId())
        );
        assertTrue(preferencesRepository.findByUser_Id(user.getId()).isEmpty());
    }

    @Test
    void savesThePreferencesAndReplacesThemOnTheNextUpdate() {
        userPreferencesService.update(user.getId(), new UpdatePreferencesRequest(7, 5, StartSection.ACTIVITIES, null));
        PreferencesResponse updated = userPreferencesService.update(
                user.getId(),
                new UpdatePreferencesRequest(1, 0, StartSection.SIMULATOR, null)
        );

        assertEquals(new PreferencesResponse(1, 0, StartSection.SIMULATOR, null), updated);
        assertEquals(updated, userPreferencesService.find(user.getId()));
        assertEquals(1L, preferencesRepository.findAll().stream()
                .filter(preferences -> preferences.getUser().getId().equals(user.getId()))
                .count());
    }

    @Test
    void savesTheGradeGoalWithTwoDecimalPlaces() {
        PreferencesResponse updated = userPreferencesService.update(
                user.getId(),
                new UpdatePreferencesRequest(3, 10, StartSection.GRADES, new BigDecimal("8.5"))
        );

        assertEquals(new BigDecimal("8.50"), updated.gradeGoal());
        assertEquals(StartSection.GRADES, updated.startSection());
    }

    @Test
    void clearsTheGradeGoalWhenItIsRemoved() {
        userPreferencesService.update(user.getId(), new UpdatePreferencesRequest(3, 10, StartSection.DASHBOARD, new BigDecimal("9")));

        PreferencesResponse updated = userPreferencesService.update(
                user.getId(),
                new UpdatePreferencesRequest(3, 10, StartSection.DASHBOARD, null)
        );

        assertNull(updated.gradeGoal());
    }

    @Test
    void keepsEachUsersPreferencesSeparate() {
        AppUser otherUser = userRepository.save(new AppUser("Outra pessoa", "outras-preferencias@example.com", "hash"));

        userPreferencesService.update(user.getId(), new UpdatePreferencesRequest(14, 20, StartSection.DISCIPLINES, new BigDecimal("7.5")));

        assertEquals(PreferencesResponse.defaults(), userPreferencesService.find(otherUser.getId()));
    }
}
