package app.project.domain;

import app.stage.domain.Stage;
import app.task.domain.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ScheduleServiceTest
{
    private static final double WORKING_HOURS_PER_DAY = 7.5;

    private final ScheduleService scheduleService = new ScheduleService();

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/project/schedule-service/calculate-finish-date.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Calculate finish date - schedules tasks on working days after their predecessors")
    void calculatesFinishDateOnWorkingDays(LocalDate startDate, Double predecessorDays, Double taskDays,
                                           LocalDate expected, String reason)
    {
        Project project = project(startDate, startDate.plusYears(1));
        Stage stage = stage(project);
        if (taskDays != null)
        {
            Task task = task(stage, "Task", taskDays);
            if (predecessorDays != null)
            {
                task.addPredecessor(task(stage, "Predecessor", predecessorDays));
            }
        }

        assertThat(scheduleService.calculateFinishDate(project).calculatedFinishDate(), is(expected));
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/project/schedule-service/calculate-feasibility.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Calculate feasibility - a plan is feasible when it finishes on or before the deadline")
    void flagsFeasibilityAgainstDeadline(LocalDate startDate, LocalDate deadline, double taskDays,
                                         String expected, String reason)
    {
        Project project = project(startDate, deadline);
        task(stage(project), "Task", taskDays);

        boolean feasible = scheduleService.calculateFinishDate(project).feasible();

        assertThat(feasible ? "FEASIBLE" : "INFEASIBLE", is(expected));
    }

    @Test
    @DisplayName("Calculate finish date - should recalculate after a duration or deadline change")
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

    // The duration is set through the estimate, so fractional days can be expressed.
    private Task task(Stage stage, String name, double durationInDays)
    {
        Task task = new Task(stage, name, 0);
        task.setCompetence(null, durationInDays * WORKING_HOURS_PER_DAY);
        return task;
    }
}
