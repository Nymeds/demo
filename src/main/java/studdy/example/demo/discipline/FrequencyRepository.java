package studdy.example.demo.discipline;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FrequencyRepository extends JpaRepository<Frequency, UUID> {

    Optional<Frequency> findByDiscipline_Id(UUID disciplineId);
}