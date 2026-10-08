package app.competence.domain;

import app.competence.presentation.dto.CompetenceCreateDTO;
import app.competence.presentation.dto.CompetenceDTO;
import app.competence.presentation.dto.CompetenceUpdateDTO;
import app.persistence.testdoubles.InMemoryCompetenceDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompetenceServiceTest
{
    private InMemoryCompetenceDAO competenceDAO;
    private CompetenceService competenceService;

    @BeforeEach
    void setUp()
    {
        competenceDAO = new InMemoryCompetenceDAO();
        Competence carpenter = competenceDAO.create(new Competence("carpenter", new BigDecimal("850.00")));
        competenceDAO.create(new Competence("electrician", new BigDecimal("900.00")));
        competenceDAO.create(new Competence("plumber", new BigDecimal("800.00"))).setActive(false);
        competenceDAO.markAsUsedByTask(carpenter.getId());
        competenceService = new CompetenceService(competenceDAO);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/competence/competence-service/create-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - validates and normalizes the name and validates the rate")
    void createValidatesFields(String name, BigDecimal rate, String expected, String reason)
    {
        CompetenceCreateDTO dto = new CompetenceCreateDTO(name, rate);

        if (expected.equals("ACCEPT"))
        {
            CompetenceDTO result = competenceService.create(dto);
            assertThat(result.name(), is(normalized(name)));
            assertThat(result.rate(), comparesEqualTo(rate));
            assertThat(result.active(), is(true));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> competenceService.create(dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/competence/competence-service/update-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - validates the competence, its name and its rate")
    void updateValidatesFields(Long id, String name, BigDecimal rate, String expected, String reason)
    {
        CompetenceUpdateDTO dto = new CompetenceUpdateDTO(name, rate);

        if (expected.equals("ACCEPT"))
        {
            competenceService.update(id, dto);
            CompetenceDTO stored = competenceService.get(id);
            assertThat(stored.name(), is(normalized(name)));
            assertThat(stored.rate(), comparesEqualTo(rate));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> competenceService.update(id, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/competence/competence-service/delete-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - requires an existing competence that no task uses")
    void deleteRequiresUnusedCompetence(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            competenceService.delete(id);
            Exception exception = assertThrows(Exception.class, () -> competenceService.get(id));
            assertThat(exception.getClass().getSimpleName(), is("NotFoundException"));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> competenceService.delete(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/competence/competence-service/set-active.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Activate and deactivate - require an existing competence")
    void setActiveRequiresExistingCompetence(Long id, boolean active, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            competenceService.setActive(id, active);
            assertThat(competenceService.get(id).active(), is(active));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> competenceService.setActive(id, active));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/competence/competence-service/get-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get - requires an existing competence")
    void getRequiresExistingCompetence(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            assertThat(competenceService.get(id).id(), is(id));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> competenceService.get(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    // The documented normalization: names are stored trimmed, with the casing they are given.
    private String normalized(String name)
    {
        return name.trim();
    }
}
