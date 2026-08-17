package studdy.example.demo.discipline;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class DisciplineGradePersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deletesGradesWhenTheirDisciplineIsDeleted() {
        AppUser user = userRepository.save(new AppUser("Estudante", "cascade@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre", DashboardStatus.ACTIVE, user));
        Discipline discipline = disciplineRepository.save(
                new Discipline(
                        "Banco de Dados",
                        "Professor João",
                        60,
                        new BigDecimal("6.00"),
                        dashboard,
                        List.of()
                )
        );
        Grade grade = gradeRepository.save(new Grade(
                discipline,
                "Prova 1",
                new BigDecimal("8.00"),
                LocalDate.of(2026, 8, 15)
        ));

        // Limpa o contexto de persistência para simular o cenário real: criar a nota
        // e excluir a disciplina acontecem em requisições (transações) separadas, então
        // a coleção discipline.grades precisa ser recarregada do banco, não reaproveitada
        // da instância em memória (que nunca foi sincronizada com a nota recém-criada).
        entityManager.flush();
        entityManager.clear();

        Discipline reloadedDiscipline = disciplineRepository.findById(discipline.getId()).orElseThrow();
        disciplineRepository.delete(reloadedDiscipline);
        disciplineRepository.flush();

        assertFalse(gradeRepository.existsById(grade.getId()));
    }
}
