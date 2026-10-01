package studdy.example.demo.auth.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrowserSessionRepository extends JpaRepository<BrowserSession, UUID> {
    Optional<BrowserSession> findByTokenHash(String tokenHash);
    List<BrowserSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
