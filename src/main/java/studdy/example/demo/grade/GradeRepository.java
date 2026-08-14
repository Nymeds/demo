package studdy.example.demo.grade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {

    List<Grade> findAllByDiscipline_IdOrderByRecordedAtDescCreatedAtDesc(UUID disciplineId);

    Optional<Grade> findByIdAndDiscipline_Id(UUID id, UUID disciplineId);
}
