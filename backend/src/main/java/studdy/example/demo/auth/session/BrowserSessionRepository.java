package studdy.example.demo.auth.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrowserSessionRepository extends JpaRepository<BrowserSession, UUID> {
    Optional<BrowserSession> findByTokenHash(String tokenHash);
    List<BrowserSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByIdAndUser_IdAndExpiresAtAfter(UUID id, UUID userId, Instant now);

    @Modifying
    @Query("delete from BrowserSession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
