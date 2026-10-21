package app.project.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;

import app.employee.domain.Employee;
import app.stage.domain.Stage;
import app.task.domain.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class CompletedTaskScheduleTest
{
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/project/completed-schedule/predecessors.csv", numLinesToSkip = 1)
    @DisplayName("Completed predecessor - recorded dates control task and stage successors")
    void successorUsesRecordedPredecessorDates(String dependency, double estimate, LocalDate recordedEnd,
                                             LocalDate expectedStart, String reason)
    {
        Project project = Project.builder().startDate(MONDAY).build();
        Stage first = new Stage("First", project);
        Stage next = dependency.equals("STAGE") ? new Stage("Next", project) : first;
        Task predecessor = new Task(first, "Completed", 0);
        predecessor.setCompetence(null, estimate);
        Employee employee = new Employee("Assigned", "Worker", 7.5);
        predecessor.assignEmployee(employee);
        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(predecessor, MONDAY);
        assertThat(dates.endDate(), is(recordedEnd));
        predecessor.recordPlanningSnapshot(dates.startDate(), dates.endDate(),
                predecessor.getLaborDurationInDays(), predecessor.getScheduledDurationInDays());
        predecessor.changeStatus(Task.TaskStatus.DONE);

        Task successor = new Task(next, "Unfinished", 1);
        if (dependency.equals("STAGE")) next.getPredecessors().add(first);
        else successor.addPredecessor(predecessor);

        employee.update("Assigned", "Worker", 1.0);
        employee.setActive(false);
        predecessor.update(null, 20);

        assertThat(WorkingDaySchedule.calculateTaskDates(predecessor, MONDAY), is(dates));
        assertThat(WorkingDaySchedule.calculateTaskDates(successor, MONDAY).startDate(), is(expectedStart));
    }

    @Test
    @DisplayName("Completed dates - a new project start and changed upstream plan do not move a snapshot")
    void recordedDatesIgnoreProjectStartAndUpstreamChanges()
    {
        Stage stage = new Stage("Stage", null);
        Task upstream = new Task(stage, "Upstream", 1);
        Task completed = new Task(stage, "Completed", 2);
        completed.addPredecessor(upstream);
        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(completed, MONDAY);
        completed.recordPlanningSnapshot(dates.startDate(), dates.endDate(), 0.0, 2.0);
        completed.changeStatus(Task.TaskStatus.DONE);

        upstream.update(null, 20);

        assertThat(WorkingDaySchedule.calculateTaskDates(completed, MONDAY.plusWeeks(1)), is(dates));
    }

    @Test
    @DisplayName("Undetermined snapshot - completed predecessor stays undetermined after staffing becomes available")
    void undeterminedSnapshotDoesNotFallBackToLiveDates()
    {
        Stage stage = new Stage("Stage", null);
        Task completed = new Task(stage, "Completed", 1);
        completed.recordPlanningSnapshot(null, null, null, null);
        completed.changeStatus(Task.TaskStatus.DONE);
        completed.assignEmployee(new Employee("Available", "Worker", 7.5));
        Task successor = new Task(stage, "Successor", 1);
        successor.addPredecessor(completed);

        assertThat(WorkingDaySchedule.calculateTaskDates(completed, MONDAY),
                is(WorkingDaySchedule.TaskDates.UNDETERMINED));
        assertThat(WorkingDaySchedule.calculateTaskDates(successor, MONDAY),
                is(WorkingDaySchedule.TaskDates.UNDETERMINED));
        assertThat(completed.getLaborDurationInDays(), nullValue());
        assertThat(completed.getScheduledDurationInDays(), nullValue());
    }

    @Test
    @DisplayName("Legacy DONE task - missing snapshot falls back to live planning until backfill")
    void legacyDoneTaskWithoutSnapshotUsesLiveCalculation()
    {
        Task task = new Task(null, "Legacy", 2);
        task.changeStatus(Task.TaskStatus.DONE);

        assertThat(task.hasPlanningSnapshot(), is(false));
        assertThat(task.getScheduledDurationInDays(), is(2.0));
        assertThat(WorkingDaySchedule.calculateTaskDates(task, MONDAY).endDate(), is(MONDAY.plusDays(1)));
    }
}
