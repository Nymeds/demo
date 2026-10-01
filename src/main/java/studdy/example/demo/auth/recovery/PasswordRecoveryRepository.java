package studdy.example.demo.auth.recovery;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PasswordRecoveryRepository extends JpaRepository<PasswordRecovery, UUID> {
}
