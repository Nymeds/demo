package studdy.example.demo.activities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActivityControllerTest {

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
    private JwtService jwtService;

    private Dashboard dashboard;
    private Discipline discipline;
    private String disciplineActivitiesUrl;
    private String dashboardActivitiesUrl;
    private String token;

    @BeforeEach
    void setUp() {
        AppUser owner = userRepository.save(
                new AppUser("Estudante", "atividade-api@example.com", "hash")
        );

        dashboard = dashboardRepository.save(
                new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner)
        );

        discipline = disciplineRepository.save(
                new Discipline(
                        "Cálculo",
                        "Prof. Teste",
                        "#4F46E5",
                        new BigDecimal("6.00"),
                        new BigDecimal("75.00"),
                        dashboard,
                        List.of(),
                        "2026.2",
                        "2"
                )
        );

        disciplineActivitiesUrl = "/api/v1/dashboards/" + dashboard.getId()
                + "/disciplines/" + discipline.getId() + "/activities";
        dashboardActivitiesUrl = "/api/v1/dashboards/" + dashboard.getId() + "/activities";
        token = jwtService.generateToken(owner.getId());
    }

    @Test
    void createsAnActivityWithoutTypeDefaultingToActivity() throws Exception {
        mockMvc.perform(authorizedPost(disciplineActivitiesUrl, activityBody(null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ACTIVITY"));
    }

    @Test
    void createsAnActivityWithExplicitExamType() throws Exception {
        mockMvc.perform(authorizedPost(disciplineActivitiesUrl, activityBody("EXAM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXAM"));
    }

    @Test
    void updateWithoutTypeKeepsTheCurrentType() throws Exception {
        String activityId = createAndGetId("EXAM");

        String payload = """
                {"title":"Alterada","description":"Nova descrição","dueDate":"%s","status":"IN_PROGRESS"}
                """.formatted(TODAY.plusDays(5));

        mockMvc.perform(put(disciplineActivitiesUrl + "/" + activityId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EXAM"));
    }

    @Test
    void updateWithTypeChangesIt() throws Exception {
        String activityId = createAndGetId(null);

        String payload = """
                {"title":"Alterada","description":"Nova descrição","dueDate":"%s","status":"IN_PROGRESS","type":"EXAM"}
                """.formatted(TODAY.plusDays(5));

        mockMvc.perform(put(disciplineActivitiesUrl + "/" + activityId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EXAM"));
    }

    @Test
    void filtersDisciplineActivitiesByType() throws Exception {
        createAndGetId("EXAM");
        createAndGetId(null);

        mockMvc.perform(get(disciplineActivitiesUrl + "?type=EXAM")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("EXAM"));

        mockMvc.perform(get(disciplineActivitiesUrl)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void filtersDashboardActivitiesByType() throws Exception {
        createAndGetId("EXAM");
        createAndGetId(null);

        mockMvc.perform(get(dashboardActivitiesUrl + "?type=EXAM")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("EXAM"));

        mockMvc.perform(get(dashboardActivitiesUrl)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsAnInvalidTypeOnDisciplineEndpointWithBadRequest() throws Exception {
        mockMvc.perform(get(disciplineActivitiesUrl + "?type=NOT_A_TYPE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O valor informado para 'type' é inválido."));
    }

    @Test
    void rejectsAnInvalidTypeOnDashboardEndpointWithBadRequest() throws Exception {
        mockMvc.perform(get(dashboardActivitiesUrl + "?type=NOT_A_TYPE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthenticationOnDashboardActivities() throws Exception {
        mockMvc.perform(get(dashboardActivitiesUrl))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownershipIsStillEnforcedWhenFilteringByType() throws Exception {
        AppUser intruder = userRepository.save(
                new AppUser("Intruso", "atividade-api-intruso@example.com", "hash")
        );
        String intruderToken = jwtService.generateToken(intruder.getId());

        mockMvc.perform(get(dashboardActivitiesUrl + "?type=EXAM")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Dashboard não encontrado."));

        mockMvc.perform(get(disciplineActivitiesUrl + "?type=EXAM")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Dashboard não encontrado."));
    }

    @Test
    void rejectsMovingDueDateAfterLinkedGradeRecordedAtWithConflict() throws Exception {
        String pastDueActivityBody = """
                {"title":"Prova 1","description":"Descrição","dueDate":"%s","status":"PENDING"}
                """.formatted(TODAY.minusDays(5));

        String activityId = mockMvc.perform(authorizedPost(disciplineActivitiesUrl, pastDueActivityBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        String gradePayload = """
                {"assessmentName":"Prova 1","score":8.00,"recordedAt":"%s","activityId":"%s"}
                """.formatted(TODAY.minusDays(3), activityId);

        mockMvc.perform(post("/api/v1/dashboards/" + dashboard.getId()
                        + "/disciplines/" + discipline.getId() + "/grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradePayload))
                .andExpect(status().isCreated());

        String updatePayload = """
                {"title":"Alterada","description":"Nova descrição","dueDate":"%s","status":"IN_PROGRESS"}
                """.formatted(TODAY.minusDays(1));

        mockMvc.perform(put(disciplineActivitiesUrl + "/" + activityId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isConflict());
    }

    private String createAndGetId(String type) throws Exception {
        String response = mockMvc.perform(authorizedPost(disciplineActivitiesUrl, activityBody(type)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    private String activityBody(String type) {
        String typeField = type == null ? "" : ",\"type\":\"" + type + "\"";
        return """
                {"title":"Prova 1","description":"Descrição","dueDate":"%s","status":"PENDING"%s}
                """.formatted(TODAY.plusDays(1), typeField);
    }

    private org.springframework.test.web.servlet.RequestBuilder authorizedPost(String url, String content) {
        return post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }
}
