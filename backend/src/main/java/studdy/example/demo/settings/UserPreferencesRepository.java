package studdy.example.demo.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

    Optional<UserPreferences> findByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);
}
