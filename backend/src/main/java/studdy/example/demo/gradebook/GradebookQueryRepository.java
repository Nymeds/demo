package studdy.example.demo.gradebook;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import studdy.example.demo.discipline.Discipline;

import java.util.List;
import java.util.UUID;

public interface GradebookQueryRepository extends Repository<Discipline, UUID> {

    // Uma consulta só para todas as disciplinas do dashboard, em vez de buscar as notas de cada
    // disciplina separadamente. A média é calculada no serviço a partir da soma, com o mesmo
    // arredondamento da tela de disciplinas.
    @Query("""
            select new studdy.example.demo.gradebook.GradebookRow(
                discipline.id,
                discipline.name,
                discipline.professorName,
                discipline.color,
                discipline.semester,
                discipline.periodo,
                discipline.passingAverage,
                sum(grade.score),
                count(grade.id),
                max(grade.recordedAt)
            )
            from Discipline discipline
            left join discipline.grades grade
            where discipline.dashboard.id = :dashboardId
            group by discipline.id, discipline.name, discipline.professorName, discipline.color,
                     discipline.semester, discipline.periodo, discipline.passingAverage
            order by discipline.name asc
            """)
    List<GradebookRow> summarizeByDashboard(@Param("dashboardId") UUID dashboardId);
}
