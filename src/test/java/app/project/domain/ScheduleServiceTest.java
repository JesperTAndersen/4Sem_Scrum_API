package app.project.domain;

import app.stage.domain.Stage;
import app.task.domain.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ScheduleServiceTest
{
    private final ScheduleService scheduleService = new ScheduleService();

    @Test
    void excludesWeekendsAndSchedulesSuccessorsAfterTheirPredecessor()
    {
        Project project = project(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 6)); // Friday
        Stage stage = stage(project);
        Task predecessor = new Task(stage, "Build", 2);
        Task successor = new Task(stage, "Inspect", 1);
        Task parallelSuccessor = new Task(stage, "Prepare", 1);
        successor.addPredecessor(predecessor);
        parallelSuccessor.addPredecessor(predecessor);

        WorkingDaySchedule.TaskDates predecessorDates = WorkingDaySchedule.calculateTaskDates(predecessor, project.getStartDate());
        WorkingDaySchedule.TaskDates successorDates = WorkingDaySchedule.calculateTaskDates(successor, project.getStartDate());
        WorkingDaySchedule.TaskDates parallelSuccessorDates = WorkingDaySchedule.calculateTaskDates(parallelSuccessor, project.getStartDate());

        assertThat(predecessorDates.startDate(), is(LocalDate.of(2026, 1, 2)));
        assertThat(predecessorDates.endDate(), is(LocalDate.of(2026, 1, 5)));
        assertThat(successorDates.startDate(), is(LocalDate.of(2026, 1, 6)));
        assertThat(successorDates.endDate(), is(LocalDate.of(2026, 1, 6)));
        assertThat(parallelSuccessorDates.startDate(), is(successorDates.startDate()));
        assertThat(scheduleService.calculateFinishDate(project).feasible(), is(true));
    }

    @Test
    void recalculatesDatesAndFeasibilityAfterADurationOrDeadlineChange()
    {
        Project project = project(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 5));
        Stage stage = stage(project);
        Task task = new Task(stage, "Build", 1);

        assertThat(scheduleService.calculateFinishDate(project).calculatedFinishDate(), is(LocalDate.of(2026, 1, 2)));
        task.update(null, 2);
        assertThat(scheduleService.calculateFinishDate(project).calculatedFinishDate(), is(LocalDate.of(2026, 1, 5)));
        assertThat(scheduleService.calculateFinishDate(project).feasible(), is(true));
    }

    @Test
    void flagsAPlanThatFinishesAfterTheDeadline()
    {
        Project project = project(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 2));
        Stage stage = stage(project);
        new Task(stage, "Build", 2);

        assertThat(scheduleService.calculateFinishDate(project).calculatedFinishDate(), is(LocalDate.of(2026, 1, 5)));
        assertThat(scheduleService.calculateFinishDate(project).feasible(), is(false));
    }

    private Project project(LocalDate startDate, LocalDate deadline)
    {
        return Project.builder().startDate(startDate).deadline(deadline).stages(new HashSet<>()).build();
    }

    private Stage stage(Project project)
    {
        Stage stage = new Stage("Stage", project);
        project.getStages().add(stage);
        return stage;
    }
}
