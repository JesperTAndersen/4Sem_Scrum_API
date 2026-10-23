package app.task.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.persistence.testdoubles.EntityIds;
import app.project.domain.Project;
import app.project.domain.ScheduleService;
import app.stage.domain.Stage;
import app.task.data.TaskMapper;
import app.task.presentation.dto.StaffingWarning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class StaffingWarningTest
{
    @ParameterizedTest(name = "[{index}] {7}")
    @CsvFileSource(resources = "/testcases/task/staffing-warning/current-crew.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Staffing warnings - unfinished tasks use active eligible capacity and preserve assignments")
    void reportsCurrentCrew(Task.TaskStatus status, boolean active, boolean eligible, boolean activeColleague,
                            Task.StaffingStatus expectedStatus, Double expectedLabor,
                            StaffingWarning.WarningType expectedWarningType, String reason)
    {
        LocalDate start = LocalDate.of(2030, 1, 7);
        Project project = Project.builder().startDate(start).deadline(start.plusMonths(1))
                .stages(new HashSet<>()).build();
        Stage stage = new Stage("Stage", project);
        project.getStages().add(stage);
        Task task = EntityIds.assign(new Task(stage, "Work", 0), 10L);
        Competence competence = EntityIds.assign(new Competence("Required", BigDecimal.TEN), 1L);
        task.setCompetence(competence, 30.0);
        Employee affected = EntityIds.assign(new Employee("Affected", "Worker", 7.5), 2L);
        affected.setActive(active);
        if (eligible) affected.addCompetence(competence);
        task.assignEmployee(affected);
        if (activeColleague)
        {
            Employee colleague = EntityIds.assign(new Employee("Active", "Worker", 7.5), 1L);
            colleague.addCompetence(competence);
            task.assignEmployee(colleague);
        }
        if (status == Task.TaskStatus.DONE) task.recordPlanningSnapshot(start, start.plusDays(1), 2.0, 2.0);
        task.changeStatus(status);

        var dto = TaskMapper.toDTO(task);
        assertEquals(expectedStatus, dto.staffingStatus());
        assertEquals(expectedLabor, dto.laborDurationInDays());
        assertEquals(activeColleague ? 2 : 1, dto.assignedEmployees().size());
        if (expectedWarningType == null)
        {
            assertEquals(List.of(), dto.staffingWarnings());
        }
        else
        {
            assertEquals(1, dto.staffingWarnings().size());
            var warning = dto.staffingWarnings().getFirst();
            assertEquals(expectedWarningType, warning.warningType());
            assertEquals(10L, warning.taskId());
            assertEquals(2L, warning.employeeId());
        }
        var schedule = new ScheduleService().calculateFinishDate(project);
        assertEquals(dto.staffingWarnings(), schedule.staffingWarnings());
        if (expectedLabor == null)
        {
            assertNull(dto.startDate());
            assertNull(dto.endDate());
            assertNull(schedule.calculatedFinishDate());
            assertNull(schedule.feasible());
        }

        affected.setActive(true);
        affected.addCompetence(competence);
        assertEquals(List.of(), TaskMapper.toDTO(task).staffingWarnings());
        assertEquals(status == Task.TaskStatus.DONE ? 2.0 : activeColleague ? 2.0 : 4.0,
                task.getLaborDurationInDays());
    }
}
