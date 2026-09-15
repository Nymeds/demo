package studdy.example.demo.grade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.activities.Activity;
import studdy.example.demo.activities.ActivityRepository;
import studdy.example.demo.activities.ActivityStatus;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GradeControllerActivityTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private JwtService jwtService;

    private Discipline discipline;
    private String url;
    private String token;

    @BeforeEach
    void setUp() {
        AppUser owner = userRepository.save(new AppUser("Estudante", "nota-atividade-api@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        discipline = disciplineRepository.save(new Discipline(
                "Banco de Dados",
                "Prof. Rafael",
                "#19B36B",
                new BigDecimal("7.00"),
                new BigDecimal("75.00"),
                dashboard,
                List.of(),
                "2026.2",
                "2"
        ));

        url = "/api/v1/dashboards/" + dashboard.getId() + "/disciplines/" + discipline.getId() + "/grades";
        token = jwtService.generateToken(owner.getId());
    }

    @Test
    void requiresAuthentication() throws Exception {
        UUID examId = newActivity(TODAY.minusDays(2)).getId();

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body(examId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsTheGradeLinkedToTheActivity() throws Exception {
        UUID examId = newActivity(TODAY.minusDays(2)).getId();

        mockMvc.perform(authorizedPost(body(examId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityId").value(examId.toString()))
                .andExpect(jsonPath("$.score").value(8.5));
    }

    @Test
    void explainsWhyAFutureActivityCannotReceiveAGrade() throws Exception {
        UUID examId = newActivity(TODAY.plusDays(3)).getId();

        mockMvc.perform(authorizedPost(body(examId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("Essa avaliação ainda não aconteceu.")));
    }

    @Test
    void explainsThatTheActivityAlreadyHasAGrade() throws Exception {
        UUID examId = newActivity(TODAY.minusDays(2)).getId();
        mockMvc.perform(authorizedPost(body(examId))).andExpect(status().isCreated());

        mockMvc.perform(authorizedPost(body(examId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Essa avaliação já tem uma nota lançada."));
    }

    private RequestBuilder authorizedPost(String content) {
        return post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private String body(UUID activityId) {
        return """
                {"assessmentName": "Prova 1", "score": 8.5, "recordedAt": "%s", "activityId": "%s"}
                """.formatted(TODAY, activityId);
    }

    private Activity newActivity(LocalDate dueDate) {
        return activityRepository.save(new Activity(
                "Prova 1",
                null,
                dueDate,
                ActivityStatus.COMPLETED,
                discipline
        ));
    }
}
