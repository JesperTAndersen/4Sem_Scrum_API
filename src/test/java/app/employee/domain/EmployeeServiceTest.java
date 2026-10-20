package app.employee.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import app.competence.domain.Competence;
import app.employee.presentation.dto.EmployeeCapacityDTO;
import app.employee.presentation.dto.EmployeeDTO;
import app.persistence.testdoubles.InMemoryCompetenceDAO;
import app.persistence.testdoubles.InMemoryEmployeeDAO;

class EmployeeServiceTest
{
    private EmployeeService employeeService;

    /**
     * Competences: 1 carpenter, 2 painter, 3 plumber (no employees).
     * Employees: Alice (carpenter), Bob (carpenter, inactive), Carl (painter), Dina (carpenter and painter).
     */
    @BeforeEach
    void setUp()
    {
        InMemoryCompetenceDAO competenceDAO = new InMemoryCompetenceDAO();
        Competence carpenter = competenceDAO.create(new Competence("carpenter", new BigDecimal("850.00")));
        Competence painter = competenceDAO.create(new Competence("painter", new BigDecimal("700.00")));
        competenceDAO.create(new Competence("plumber", new BigDecimal("800.00")));

        InMemoryEmployeeDAO employeeDAO = new InMemoryEmployeeDAO();
        employeeDAO.create(new Employee("Alice", "Worker", 7.5)).addCompetence(carpenter);
        Employee bob = employeeDAO.create(new Employee("Bob", "Builder", 7.5));
        bob.addCompetence(carpenter);
        bob.setActive(false);
        employeeDAO.create(new Employee("Carl", "Painter", 6.0)).addCompetence(painter);
        Employee dina = employeeDAO.create(new Employee("Dina", "Allround", 5.0));
        dina.addCompetence(carpenter);
        dina.addCompetence(painter);

        // The company capacity DAO is only used when creating and updating employees.
        employeeService = new EmployeeService(employeeDAO, null, competenceDAO);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/employee/employee-service/get-all-by-competence.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get all - filters employees by an optional competence")
    void getAllFiltersByCompetence(Long competenceId, String expectedNames, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            List<String> names = employeeService.getAll(competenceId).stream()
                    .map(EmployeeDTO::firstName)
                    .toList();
            assertThat(names, is(parseNames(expectedNames)));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> employeeService.getAll(competenceId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @Test
    @DisplayName("Get all - without arguments returns every employee")
    void getAllWithoutArgumentsReturnsEveryEmployee()
    {
        assertThat(employeeService.getAll().size(), is(4));
    }

    @Test
    @DisplayName("Capacity - counts only active employees holding the competence")
    void capacityCountsActiveEmployeesWithCompetence()
    {
        EmployeeCapacityDTO capacity = employeeService.calculateDailyCapacity(1L);

        assertThat(capacity.employeeCount(), is(2));
        assertThat(capacity.dailyCapacity(), is(12.5));
    }

    private static List<String> parseNames(String names)
    {
        return names.equals("EMPTY") ? List.of() : Arrays.asList(names.split(";"));
    }
}
