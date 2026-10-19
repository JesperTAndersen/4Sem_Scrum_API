package app.project.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.HashSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.employee.domain.Employee;
import app.project.presentation.dto.ScheduleDTO;
import app.stage.data.StageMapper;
import app.stage.domain.Stage;
import app.stage.presentation.dto.StageDTO;
import app.task.domain.Task;

class UndeterminedScheduleTest
{
    // 2030-01-07 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final LocalDate FRIDAY = LocalDate.of(2030, 1, 4);

    @Test
    @DisplayName("Task dates - undetermined when the task has no available capacity")
    void taskWithoutAvailableCapacityHasNoDates()
    {
        Stage stage = stage(project(MONDAY), "Stage");
        Task task = unstaffableTask(stage, "No capacity");

        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(task, MONDAY);

        assertThat(dates.isDetermined(), is(false));
        assertThat(dates.startDate(), is(nullValue()));
        assertThat(dates.endDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Task dates - a successor of an undetermined task is undetermined")
    void successorOfUndeterminedTaskIsUndetermined()
    {
        Stage stage = stage(project(MONDAY), "Stage");
        Task predecessor = unstaffableTask(stage, "No capacity");
        Task successor = staffedTask(stage, "Successor", 7.5, 7.5);
        successor.addPredecessor(predecessor);

        assertThat(WorkingDaySchedule.calculateTaskDates(successor, MONDAY).isDetermined(), is(false));
    }

    @Test
    @DisplayName("Task dates - tasks in a dependent stage are undetermined when a predecessor stage is undetermined")
    void dependentStageIsUndeterminedAfterUndeterminedPredecessorStage()
    {
        Project project = project(MONDAY);
        Stage first = stage(project, "First");
        Stage next = stage(project, "Next");
        next.getPredecessors().add(first);
        unstaffableTask(first, "No capacity");
        Task work = staffedTask(next, "Work", 7.5, 7.5);

        assertThat(WorkingDaySchedule.calculateTaskDates(work, MONDAY).isDetermined(), is(false));
    }

    @Test
    @DisplayName("Task dates - a staffed task skips the weekend")
    void staffedTaskSkipsWeekend()
    {
        Stage stage = stage(project(FRIDAY), "Stage");
        Task task = staffedTask(stage, "Two days", 15.0, 7.5);

        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(task, FRIDAY);

        assertThat(dates.startDate(), is(FRIDAY));
        assertThat(dates.endDate(), is(MONDAY));
    }

    @Test
    @DisplayName("Stage dates - start is the earliest known start and end is undetermined")
    void stageWithUndeterminedTaskHasNoEndDate()
    {
        Stage stage = stage(project(MONDAY), "Stage");
        staffedTask(stage, "Known", 7.5, 7.5);
        unstaffableTask(stage, "No capacity");

        StageDTO dto = StageMapper.toDTO(stage);

        assertThat(dto.startDate(), is(MONDAY));
        assertThat(dto.endDate(), is(nullValue()));
    }

    @Test
    @DisplayName("Project schedule - finish date and feasibility are undetermined when a task is undetermined")
    void projectWithUndeterminedTaskHasUndeterminedSchedule()
    {
        Project project = project(MONDAY);
        Stage stage = stage(project, "Stage");
        staffedTask(stage, "Known", 7.5, 7.5);
        unstaffableTask(stage, "No capacity");

        ScheduleDTO schedule = new ScheduleService().calculateFinishDate(project);

        assertThat(schedule.calculatedFinishDate(), is(nullValue()));
        assertThat(schedule.feasible(), is(nullValue()));
    }

    @Test
    @DisplayName("Project schedule - a fully staffed project is still calculated as before")
    void staffedProjectHasDeterminedSchedule()
    {
        Project project = project(MONDAY);
        staffedTask(stage(project, "Stage"), "Known", 15.0, 7.5);

        ScheduleDTO schedule = new ScheduleService().calculateFinishDate(project);

        assertThat(schedule.calculatedFinishDate(), is(MONDAY.plusDays(1)));
        assertThat(schedule.feasible(), is(true));
    }

    private static Project project(LocalDate startDate)
    {
        return Project.builder()
                .id(1L)
                .startDate(startDate)
                .deadline(startDate.plusYears(1))
                .stages(new HashSet<>())
                .build();
    }

    private static Stage stage(Project project, String name)
    {
        Stage stage = new Stage(name, project);
        project.getStages().add(stage);
        return stage;
    }

    private static Task staffedTask(Stage stage, String name, double estimate, double dailyCapacity)
    {
        Task task = new Task(stage, name, 0);
        task.setCompetence(null, estimate);
        task.assignEmployee(new Employee("Active", "Employee", dailyCapacity));
        return task;
    }

    private static Task unstaffableTask(Stage stage, String name)
    {
        Task task = new Task(stage, name, 0);
        task.setCompetence(null, 30.0);
        Employee inactive = new Employee("Inactive", "Employee", 7.5);
        inactive.setActive(false);
        task.assignEmployee(inactive);
        return task;
    }
}
