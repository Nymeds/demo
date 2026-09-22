package studdy.example.demo.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfilePhotoRepository extends JpaRepository<UserProfilePhoto, UUID> {

    Optional<UserProfilePhoto> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);
}
