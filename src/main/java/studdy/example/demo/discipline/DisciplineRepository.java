package studdy.example.demo.discipline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisciplineRepository extends JpaRepository<Discipline, UUID> {

    List<Discipline> findAllByDashboard_IdOrderByNameAsc(UUID dashboardId);

    Optional<Discipline> findByIdAndDashboard_Id(UUID id, UUID dashboardId);
}
