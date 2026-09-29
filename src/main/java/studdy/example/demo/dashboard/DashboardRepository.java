package studdy.example.demo.dashboard;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import studdy.example.demo.user.AppUser;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    /**
     * Bloqueia a linha do dono (SELECT ... FOR UPDATE): serializa a criação de dashboards da
     * mesma conta, para que dois "ativos" simultâneos não acabem os dois ATIVOS.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :ownerId")
    Optional<AppUser> findOwnerForUpdate(@Param("ownerId") UUID ownerId);

    Optional<Dashboard> findByIdAndOwner_Id(UUID id, UUID ownerId);

    List<Dashboard> findAllByOwner_IdOrderByNameAsc(UUID ownerId);

    List<Dashboard> findAllByOwner_IdAndStatusAndIdNot(UUID ownerId, DashboardStatus status, UUID id);

    List<Dashboard> findAllByOwner_IdAndStatus(UUID ownerId, DashboardStatus status);

    @Query("select d.owner.id from Dashboard d where d.status = :status group by d.owner.id having count(d) > 1")
    List<UUID> findOwnerIdsWithMoreThanOneDashboardInStatus(@Param("status") DashboardStatus status);

    interface DashboardDisciplineCount {
        UUID getDashboardId();

        long getDisciplineCount();
    }

    @Query("select d.id as dashboardId, count(disc) as disciplineCount "
            + "from Dashboard d left join Discipline disc on disc.dashboard = d "
            + "where d.owner.id = :ownerId and d.status = :status group by d.id")
    List<DashboardDisciplineCount> countDisciplinesByOwnerAndStatus(
            @Param("ownerId") UUID ownerId, @Param("status") DashboardStatus status);
}
