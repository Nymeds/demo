package studdy.example.demo.avatar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, UUID> {

    Optional<UserAvatar> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);
}
