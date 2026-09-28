package studdy.example.demo.discipline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AbsenceRecordRepository extends JpaRepository<AbsenceRecord, UUID> {
    boolean existsByDiscipline_Id(UUID disciplineId);

    List<AbsenceRecord> findAllByDiscipline_IdOrderByAbsenceDateDescCreatedAtDesc(UUID disciplineId);

    Optional<AbsenceRecord> findByIdAndDiscipline_Id(UUID id, UUID disciplineId);
}
