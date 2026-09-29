package studdy.example.demo.discipline;

import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import studdy.example.demo.discipline.dto.CreateFrequencyRequest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrequencyConcurrencyTest {

    @Test
    void mapsAUniqueViolationOnFrequencyInsertTo409() {
        FrequencyRepository frequencyRepository = mock(FrequencyRepository.class);
        DisciplineAccessService access = mock(DisciplineAccessService.class);
        DisciplineRepository disciplineRepository = mock(DisciplineRepository.class);
        UUID disciplineId = UUID.randomUUID();
        Discipline discipline = mock(Discipline.class);
        when(disciplineRepository.findByIdForUpdate(disciplineId)).thenReturn(Optional.of(discipline));
        when(frequencyRepository.findByDiscipline_Id(disciplineId)).thenReturn(Optional.empty());
        when(frequencyRepository.saveAndFlush(any(Frequency.class)))
                .thenThrow(new DataIntegrityViolationException("uk_frequency_discipline_id"));

        FrequencyService service = new FrequencyService(frequencyRepository, access, disciplineRepository);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                service.create(UUID.randomUUID(), UUID.randomUUID(), disciplineId, new CreateFrequencyRequest(1)));
        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void frequencyDeclaresTheNamedUniqueConstraint() {
        Table table = Frequency.class.getAnnotation(Table.class);
        assertEquals("uk_frequency_discipline_id", table.uniqueConstraints()[0].name());
    }
}
