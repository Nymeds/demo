package studdy.example.demo.activities;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Bloqueia a atividade (SELECT ... FOR UPDATE): serializa o lançamento de nota com a edição do prazo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activity a where a.id = :id and a.discipline.id = :disciplineId")
    Optional<Activity> findByIdAndDiscipline_IdForUpdate(@Param("id") UUID id, @Param("disciplineId") UUID disciplineId);

    Optional<Activity> findByIdAndDiscipline_Id(
            UUID id,
            UUID disciplineId
    );
}
