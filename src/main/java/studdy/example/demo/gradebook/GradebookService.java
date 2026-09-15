package studdy.example.demo.gradebook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.discipline.DisciplineAccessService;
import studdy.example.demo.gradebook.dto.GradebookEntryResponse;

import java.util.List;
import java.util.UUID;

@Service
public class GradebookService {

    private final DisciplineAccessService disciplineAccessService;
    private final GradebookQueryRepository gradebookQueryRepository;

    public GradebookService(
            DisciplineAccessService disciplineAccessService,
            GradebookQueryRepository gradebookQueryRepository
    ) {
        this.disciplineAccessService = disciplineAccessService;
        this.gradebookQueryRepository = gradebookQueryRepository;
    }

    // Dashboard de outra pessoa responde 404, como no resto da API.
    @Transactional(readOnly = true)
    public List<GradebookEntryResponse> findAll(UUID userId, UUID dashboardId) {
        disciplineAccessService.findOwnedDashboard(userId, dashboardId);

        return gradebookQueryRepository.summarizeByDashboard(dashboardId).stream()
                .map(GradebookEntryResponse::from)
                .toList();
    }
}
