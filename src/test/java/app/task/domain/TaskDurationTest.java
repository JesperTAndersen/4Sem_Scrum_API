package app.task.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import app.capacity.domain.CompanyCapacity;
import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.task.domain.Task.StaffingStatus;

class TaskDurationTest
{
    private static final double TOLERANCE = 1e-9;

    @ParameterizedTest(name = "[{index}] {7}")
    @CsvFileSource(resources = "/testcases/task/task-duration/labor-duration.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Labor duration - derived from the daily capacity of active assigned employees")
    void calculatesDurationFromAssignedCapacity(double estimate, String capacities, String activeFlags, int minimumDays,
                                                Double expectedLabor, Double expectedScheduled,
                                                StaffingStatus expectedStatus, String reason)
    {
        Task task = task(estimate, minimumDays);
        if (!capacities.equals("NONE"))
        {
            String[] capacityValues = capacities.split(";");
            String[] activeValues = activeFlags.split(";");
            for (int i = 0; i < capacityValues.length; i++)
            {
                Employee employee = employee(Double.parseDouble(capacityValues[i]));
                employee.setActive(Boolean.parseBoolean(activeValues[i]));
                task.assignEmployee(employee);
            }
        }

        assertThat(task.getStaffingStatus(), is(expectedStatus));
        assertDuration(task.getLaborDurationInDays(), expectedLabor);
        assertDuration(task.getScheduledDurationInDays(), expectedScheduled);
    }

    @Test
    @DisplayName("Labor duration - employees using the company standard capacity contribute that capacity")
    void standardCapacityEmployeeUsesCompanySetting()
    {
        Task task = task(30.0, 0);
        Employee employee = employee(0.0);
        employee.configureCapacity(true, new CompanyCapacity());
        task.assignEmployee(employee);

        assertThat(task.getLaborDurationInDays(), closeTo(4.0, TOLERANCE));
    }

    @Test
    @DisplayName("Labor duration - an employee with several competences contributes once")
    void employeeWithSeveralCompetencesContributesOnce()
    {
        Task task = task(30.0, 0);
        Employee employee = employee(7.5);
        employee.addCompetence(new Competence("carpenter", new BigDecimal("850.00")));
        employee.addCompetence(new Competence("painter", new BigDecimal("700.00")));
        task.assignEmployee(employee);
        task.assignEmployee(employee);

        assertThat(task.getLaborDurationInDays(), closeTo(4.0, TOLERANCE));
    }

    @Test
    @DisplayName("Labor duration - a completed task keeps its duration when its employees are deactivated")
    void completedTaskKeepsDurationAfterDeactivation()
    {
        Task task = task(30.0, 0);
        Employee employee = employee(7.5);
        task.assignEmployee(employee);
        task.changeStatus(Task.TaskStatus.DONE);

        employee.setActive(false);

        assertThat(task.getStaffingStatus(), is(StaffingStatus.STAFFED));
        assertThat(task.getLaborDurationInDays(), closeTo(4.0, TOLERANCE));
    }

    @Test
    @DisplayName("Dependency offsets - undetermined when a predecessor has no available capacity")
    void dependencyOffsetsAreUndeterminedAfterAnUndeterminedPredecessor()
    {
        Task predecessor = task(30.0, 0);
        Employee inactive = employee(7.5);
        inactive.setActive(false);
        predecessor.assignEmployee(inactive);
        Task task = task(7.5, 0);
        task.addPredecessor(predecessor);

        assertThat(predecessor.getDependencyFinishOffsetInDays(), is(nullValue()));
        assertThat(task.getDependencyStartOffsetInDays(), is(nullValue()));
        assertThat(task.getDependencyFinishOffsetInDays(), is(nullValue()));
    }

    private static Task task(double estimate, int minimumDays)
    {
        Task task = new Task(null, "Task", minimumDays);
        task.setCompetence(null, estimate);
        return task;
    }

    private static Employee employee(double dailyCapacity)
    {
        return new Employee("Test", "Employee", dailyCapacity);
    }

    private static void assertDuration(Double actual, Double expected)
    {
        if (expected == null)
        {
            assertThat(actual, is(nullValue()));
        }
        else
        {
            assertThat(actual, closeTo(expected, TOLERANCE));
        }
    }
}
