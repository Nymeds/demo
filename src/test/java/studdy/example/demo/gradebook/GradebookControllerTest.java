package studdy.example.demo.gradebook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import studdy.example.demo.dashboard.Dashboard;
import studdy.example.demo.dashboard.DashboardRepository;
import studdy.example.demo.dashboard.DashboardStatus;
import studdy.example.demo.discipline.Discipline;
import studdy.example.demo.discipline.DisciplineRepository;
import studdy.example.demo.grade.Grade;
import studdy.example.demo.grade.GradeRepository;
import studdy.example.demo.security.JwtService;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GradebookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private JwtService jwtService;

    private String url;
    private String token;

    @BeforeEach
    void setUp() {
        AppUser owner = userRepository.save(new AppUser("Estudante", "notas-api@example.com", "hash"));
        Dashboard dashboard = dashboardRepository.save(new Dashboard("Semestre 2026.2", DashboardStatus.ACTIVE, owner));
        Discipline discipline = disciplineRepository.save(new Discipline(
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
        gradeRepository.save(new Grade(discipline, "Prova 1", new BigDecimal("8.00"), LocalDate.of(2026, 8, 10)));
        gradeRepository.save(new Grade(discipline, "Trabalho", new BigDecimal("9.00"), LocalDate.of(2026, 8, 20)));

        url = "/api/v1/dashboards/" + dashboard.getId() + "/gradebook";
        token = jwtService.generateToken(owner.getId());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheAverageAndTheBandOfEachDiscipline() throws Exception {
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Banco de Dados"))
                .andExpect(jsonPath("$[0].average").value(8.5))
                .andExpect(jsonPath("$[0].gradeCount").value(2))
                .andExpect(jsonPath("$[0].band").value("GOOD"))
                .andExpect(jsonPath("$[0].passing").value(true))
                .andExpect(jsonPath("$[0].lastRecordedAt").value("2026-08-20"));
    }

    @Test
    void hidesTheDashboardOfAnotherUser() throws Exception {
        AppUser intruder = userRepository.save(new AppUser("Intruso", "intruso-notas@example.com", "hash"));

        mockMvc.perform(get(url).header("Authorization", "Bearer " + jwtService.generateToken(intruder.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnInvalidDashboardId() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/nao-e-um-uuid/gradebook").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
