package studdy.example.demo.grade;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {

    List<Grade> findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(UUID disciplineId);

    Optional<Grade> findByIdAndDiscipline_Id(UUID id, UUID disciplineId);

    boolean existsByActivity_Id(UUID activityId);

    boolean existsByActivity_IdAndIdNot(UUID activityId, UUID id);

    Optional<Grade> findByActivity_Id(UUID activityId);

    /** Bloqueia a nota vinculada (SELECT ... FOR UPDATE) enquanto o prazo da atividade é validado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Grade g where g.activity.id = :activityId")
    Optional<Grade> findByActivity_IdForUpdate(@Param("activityId") UUID activityId);
}
