package studdy.example.demo.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    // Serializa operações de sessão do usuário (renovação/criação de sessão x troca de senha/e-mail).
    // Ordem de travas em todo o código: usuário primeiro, depois as sessões e a recuperação de senha.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.email = :email")
    Optional<AppUser> findByEmailForUpdate(@Param("email") String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByUsernameAndIdNot(String username, UUID id);
}
