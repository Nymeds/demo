package studdy.example.demo.discipline;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface FrequencyRepository extends JpaRepository<Frequency, UUID> {

    Optional<Frequency> findByDiscipline_Id(UUID disciplineId);

    /**
     * Bloqueia a linha de Frequency da disciplina (SELECT ... FOR UPDATE) para o
     * restante da transação. Usado em create/delete/update de faltas para evitar
     * que leituras/escritas concorrentes do total percam atualizações
     * (read-modify-write) quando múltiplas requisições alteram a mesma disciplina
     * ao mesmo tempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Frequency f where f.discipline.id = :disciplineId")
    Optional<Frequency> findByDiscipline_IdForUpdate(UUID disciplineId);
}