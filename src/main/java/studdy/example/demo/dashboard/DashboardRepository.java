package studdy.example.demo.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    Optional<Dashboard> findByIdAndOwner_Id(UUID id, UUID ownerId);
}
