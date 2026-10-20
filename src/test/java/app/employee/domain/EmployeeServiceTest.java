package app.employee.domain;

import app.capacity.domain.CompanyCapacityService;
import app.capacity.presentation.dto.UpdateCompanyCapacityDTO;
import app.competence.domain.Competence;
import app.competence.presentation.dto.CompetenceDTO;
import app.employee.presentation.dto.EmployeeCapacityDTO;
import app.employee.presentation.dto.EmployeeCompetencesUpdateDTO;
import app.employee.presentation.dto.EmployeeCreateDTO;
import app.employee.presentation.dto.EmployeeDTO;
import app.employee.presentation.dto.EmployeeUpdateDTO;
import app.exceptions.ApiException;
import app.exceptions.NotFoundException;
import app.persistence.testdoubles.InMemoryCompanyCapacityDAO;
import app.persistence.testdoubles.InMemoryCompetenceDAO;
import app.persistence.testdoubles.InMemoryEmployeeDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeServiceTest
{
    private InMemoryEmployeeDAO employeeDAO;
    private InMemoryCompetenceDAO competenceDAO;
    private InMemoryCompanyCapacityDAO companyCapacityDAO;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp()
    {
        employeeDAO = new InMemoryEmployeeDAO();
        competenceDAO = new InMemoryCompetenceDAO();
        companyCapacityDAO = new InMemoryCompanyCapacityDAO();
        competenceDAO.create(new Competence("Carpenter", new BigDecimal("850.00")));
        competenceDAO.create(new Competence("Electrician", new BigDecimal("900.00")));
        competenceDAO.create(new Competence("Plumber", new BigDecimal("800.00"))).setActive(false);
        competenceDAO.create(new Competence("Painter", new BigDecimal("700.00")));
        employeeService = new EmployeeService(employeeDAO, companyCapacityDAO, competenceDAO);
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/create-fields.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - registers an active employee with valid names and effective daily capacity")
    void createValidatesFields(boolean payloadPresent, String firstName, String lastName, Double dailyCapacity,
                              Boolean standardCapacity, String expected, String reason)
    {
        EmployeeCreateDTO dto = payloadPresent
                ? new EmployeeCreateDTO(firstName, lastName, dailyCapacity, standardCapacity) : null;
        int countBefore = employeeService.getAll().size();
        if (expected.equals("ACCEPT"))
        {
            EmployeeDTO result = employeeService.create(dto);
            assertThat(result.firstName(), is(firstName.trim()));
            assertThat(result.lastName(), is(lastName.trim()));
            assertThat(result.active(), is(true));
            assertThat(result.competences(), is(empty()));
            assertThat(result.standardCapacity(), is(Boolean.TRUE.equals(standardCapacity)));
            assertThat(result.dailyCapacity(), is(Boolean.TRUE.equals(standardCapacity) ? 7.5 : dailyCapacity));
            assertThat(employeeService.get(result.id()), is(result));
            assertThat(employeeService.getAll().size(), is(countBefore + 1));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> employeeService.create(dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(employeeService.getAll().size(), is(countBefore));
        }
    }

    @ParameterizedTest(name = "[{index}] {8}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/update-fields.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - validates fields and preserves competencies and active status")
    void updateValidatesFields(Long id, boolean payloadPresent, boolean existingStandard, String firstName,
                              String lastName, Double dailyCapacity, Boolean standardCapacity,
                              String expected, String reason)
    {
        EmployeeDTO original = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, existingStandard));
        employeeService.updateCompetences(original.id(), new EmployeeCompetencesUpdateDTO(List.of(1L, 2L)));
        employeeService.setActive(original.id(), false);
        EmployeeDTO before = employeeService.get(original.id());
        EmployeeUpdateDTO dto = payloadPresent
                ? new EmployeeUpdateDTO(firstName, lastName, dailyCapacity, standardCapacity) : null;
        if (expected.equals("ACCEPT"))
        {
            EmployeeDTO result = employeeService.update(id, dto);
            boolean usesStandard = standardCapacity == null ? existingStandard : standardCapacity;
            assertThat(result.firstName(), is(firstName.trim()));
            assertThat(result.lastName(), is(lastName.trim()));
            assertThat(result.standardCapacity(), is(usesStandard));
            assertThat(result.dailyCapacity(), is(usesStandard ? 7.5 : dailyCapacity));
            assertThat(result.active(), is(false));
            assertThat(result.competences(), is(before.competences()));
            assertThat(employeeService.get(id), is(result));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> employeeService.update(id, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(employeeService.get(original.id()), is(before));
        }
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/update-competences.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update competencies - replaces and deduplicates valid associations without partial changes")
    void updateCompetencesValidatesSelection(Long id, boolean payloadPresent, String existingIds, String selectedIds,
                                            String resultingIds, String expected, String reason)
    {
        EmployeeDTO employee = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, false));
        ids(existingIds).forEach(competenceId -> employeeDAO.get(employee.id())
                .addCompetence(competenceDAO.get(competenceId)));
        EmployeeDTO before = employeeService.get(employee.id());
        EmployeeCompetencesUpdateDTO dto = payloadPresent
                ? new EmployeeCompetencesUpdateDTO(ids(selectedIds)) : null;
        if (expected.equals("ACCEPT"))
        {
            EmployeeDTO result = employeeService.updateCompetences(id, dto);
            assertThat(result.competences().stream().map(CompetenceDTO::id).toList(), is(ids(resultingIds)));
            assertThat(result.firstName(), is(before.firstName()));
            assertThat(result.dailyCapacity(), is(before.dailyCapacity()));
            assertThat(result.active(), is(before.active()));
            assertThat(employeeService.get(id), is(result));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> employeeService.updateCompetences(id, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(employeeService.get(employee.id()), is(before));
        }
    }

    @ParameterizedTest(name = "[{index}] {7}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/calculate-daily-capacity.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Calculate daily capacity - counts each active matching employee once")
    void calculateDailyCapacityFiltersEmployees(boolean populated, boolean firstActive, boolean secondActive,
                                               Long competenceId, int employeeCount, double dailyCapacity,
                                               String expected, String reason)
    {
        if (populated)
        {
            EmployeeDTO first = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, false));
            employeeService.updateCompetences(first.id(), new EmployeeCompetencesUpdateDTO(List.of(1L, 2L, 1L)));
            employeeDAO.get(first.id()).addCompetence(competenceDAO.get(3L));
            employeeService.setActive(first.id(), firstActive);
            EmployeeDTO second = employeeService.create(new EmployeeCreateDTO("Bob", "Worker", null, true));
            employeeService.updateCompetences(second.id(), new EmployeeCompetencesUpdateDTO(List.of(1L)));
            employeeService.setActive(second.id(), secondActive);
            employeeService.create(new EmployeeCreateDTO("Cara", "Worker", 2.0, false));
        }
        if (expected.equals("ACCEPT"))
        {
            EmployeeCapacityDTO result = employeeService.calculateDailyCapacity(competenceId);
            assertThat(result.competenceId(), is(competenceId));
            assertThat(result.employeeCount(), is(employeeCount));
            assertThat(result.dailyCapacity(), is(closeTo(dailyCapacity, 0.000001)));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class,
                    () -> employeeService.calculateDailyCapacity(competenceId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/company-capacity-changes.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Company capacity changes - affect standard employees while preserving custom hours")
    void companyCapacityChangesAffectStandardEmployees(boolean standardCapacity, double companyHours,
                                                      double employeeHours, String expected, String reason)
    {
        EmployeeDTO employee = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, standardCapacity));
        CompanyCapacityService companyCapacityService = new CompanyCapacityService(companyCapacityDAO);
        if (expected.equals("ACCEPT"))
        {
            companyCapacityService.update(new UpdateCompanyCapacityDTO(companyHours));
            assertThat(companyCapacityService.get().dailyCapacity(), is(companyHours));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class,
                    () -> companyCapacityService.update(new UpdateCompanyCapacityDTO(companyHours)));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(companyCapacityService.get().dailyCapacity(), is(7.5));
        }
        assertThat(employeeService.get(employee.id()).dailyCapacity(), is(employeeHours));
        assertThat(employeeService.calculateDailyCapacity(null).dailyCapacity(), is(employeeHours));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/get-by-id.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get - requires an existing employee with a positive ID")
    void getRequiresExistingEmployee(Long id, String expected, String reason)
    {
        EmployeeDTO employee = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, false));
        if (expected.equals("ACCEPT"))
        {
            assertThat(employeeService.get(id), is(employee));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> employeeService.get(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/delete-by-id.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - removes an existing employee and their capacity contribution")
    void deleteRequiresExistingEmployee(Long id, String expected, String reason)
    {
        EmployeeDTO employee = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, false));
        if (expected.equals("ACCEPT"))
        {
            employeeService.delete(id);
            assertThrows(NotFoundException.class, () -> employeeService.get(id));
            assertThat(employeeService.getAll(), is(empty()));
            assertThat(employeeService.calculateDailyCapacity(null).dailyCapacity(), is(0.0));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> employeeService.delete(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(employeeService.get(employee.id()), is(employee));
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/set-active.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Set active - changes availability while preserving employee information and competencies")
    void setActiveChangesAvailability(Long id, boolean initiallyActive, boolean active, String expected, String reason)
    {
        EmployeeDTO employee = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, false));
        employeeService.updateCompetences(employee.id(), new EmployeeCompetencesUpdateDTO(List.of(1L, 2L)));
        employeeService.setActive(employee.id(), initiallyActive);
        EmployeeDTO before = employeeService.get(employee.id());
        if (expected.equals("ACCEPT"))
        {
            employeeService.setActive(id, active);
            EmployeeDTO result = employeeService.get(employee.id());
            boolean resultingActive = id.equals(employee.id()) ? active : initiallyActive;
            assertThat(result.active(), is(resultingActive));
            assertThat(result.firstName(), is(before.firstName()));
            assertThat(result.dailyCapacity(), is(before.dailyCapacity()));
            assertThat(result.competences(), is(before.competences()));
            assertThat(employeeService.calculateDailyCapacity(null).dailyCapacity(), is(resultingActive ? 6.0 : 0.0));
            assertThat(employeeService.calculateDailyCapacity(1L).employeeCount(), is(resultingActive ? 1 : 0));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> employeeService.setActive(id, active));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(employeeService.get(employee.id()), is(before));
        }
    }

    @Test
    @DisplayName("Get all - maps both active and inactive employees")
    void getAllIncludesInactiveEmployees()
    {
        assertThat(employeeService.getAll(), is(empty()));
        EmployeeDTO first = employeeService.create(new EmployeeCreateDTO("Alice", "Worker", 6.0, false));
        EmployeeDTO second = employeeService.create(new EmployeeCreateDTO("Bob", "Worker", null, true));
        employeeService.setActive(second.id(), false);
        assertThat(employeeService.getAll(), contains(first, employeeService.get(second.id())));
    }

    private List<Long> ids(String value)
    {
        if (value == null) return null;
        if (value.isEmpty()) return List.of();
        return Arrays.stream(value.split(";"))
                .map(id -> id.equals("NULL") ? null : Long.valueOf(id))
                .toList();
    }
}
