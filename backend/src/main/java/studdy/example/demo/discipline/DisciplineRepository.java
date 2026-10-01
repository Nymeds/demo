package studdy.example.demo.discipline;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisciplineRepository extends JpaRepository<Discipline, UUID> {

    /** Traz a frequência no mesmo SELECT (evita 1 consulta por disciplina); notas e horários vão em lote (@BatchSize). */
    @Query("select d from Discipline d left join fetch d.frequency where d.dashboard.id = :dashboardId order by d.name asc")
    List<Discipline> findAllByDashboard_IdOrderByNameAsc(@Param("dashboardId") UUID dashboardId);

    Optional<Discipline> findByIdAndDashboard_Id(UUID id, UUID dashboardId);

    /**
     * Bloqueia a linha da disciplina (SELECT ... FOR UPDATE). Serializa o "find-or-create" da
     * Frequency: na primeira falta ainda nao existe linha de Frequency para travar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Discipline d where d.id = :id")
    Optional<Discipline> findByIdForUpdate(@Param("id") UUID id);
}
