package studdy.example.demo.exams;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository
        extends JpaRepository<Exam, UUID> {

    List<Exam>
    findAllByDiscipline_Dashboard_IdOrderByDateAscStartTimeAsc(
        UUID dashboardId
    );

    Optional<Exam>
    findByIdAndDiscipline_Dashboard_Id(
        UUID id,
        UUID dashboardId
    );

}