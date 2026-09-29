package studdy.example.demo.activities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findAllByDiscipline_IdOrderByDueDateAsc(UUID disciplineId);

    List<Activity> findAllByDiscipline_IdAndTypeOrderByDueDateAsc(
            UUID disciplineId,
            ActivityType type
    );

    List<Activity> findAllByDiscipline_Dashboard_IdOrderByDueDateAscIdAsc(UUID dashboardId);

    List<Activity> findAllByDiscipline_Dashboard_IdAndTypeOrderByDueDateAscIdAsc(
            UUID dashboardId,
            ActivityType type
    );

    Optional<Activity> findByIdAndDiscipline_Id(
            UUID id,
            UUID disciplineId
    );
}
