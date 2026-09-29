package studdy.example.demo.user;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LegacyUserAvatarRepository extends JpaRepository<LegacyUserAvatar, UUID> {

    // Página (por id crescente, a partir de "after") das fotos antigas cujo usuário ainda existe e
    // ainda não tem foto nova. Linhas órfãs (usuário apagado) nunca são migradas.
    @Query("select a from LegacyUserAvatar a where a.id > :after "
            + "and exists (select u.id from AppUser u where u.id = a.userId) "
            + "and not exists (select p.id from UserProfilePhoto p where p.user.id = a.userId) "
            + "order by a.id")
    List<LegacyUserAvatar> findMigrationPage(@Param("after") UUID after, Limit limit);

    // Linhas cuja foto já existe no modelo novo (copiadas ou substituídas depois) não servem mais.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LegacyUserAvatar a where exists "
            + "(select p.id from UserProfilePhoto p where p.user.id = a.userId)")
    int deleteAllWithProfilePhoto();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LegacyUserAvatar a where a.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
