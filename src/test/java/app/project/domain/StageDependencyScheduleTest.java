package app.project.domain;

import app.stage.domain.Stage;
import app.task.domain.Task;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class StageDependencyScheduleTest
{
    @Test
    void multiplePredecessorStagesUseLatestFinishAndSkipWeekend()
    {
        Project project = Project.builder().id(1L).startDate(LocalDate.of(2030, 1, 7)).build();
        Stage first = new Stage("First", project);
        Stage second = new Stage("Second", project);
        Stage dependent = new Stage("Dependent", project);
        Task shortWork = new Task(first, "Short", 2);
        Task longWork = new Task(second, "Long", 4);
        Task following = new Task(dependent, "Following", 1);
        dependent.getPredecessors().add(first);
        dependent.getPredecessors().add(second);
        assertEquals(LocalDate.of(2030, 1, 11), WorkingDaySchedule.calculateTaskDates(following, project.getStartDate()).startDate());
        longWork.update(null, 5);
        assertEquals(LocalDate.of(2030, 1, 14), WorkingDaySchedule.calculateTaskDates(following, project.getStartDate()).startDate());
    }

    @Test
    void dependentStageWaitsEvenWithoutDirectTaskPredecessors()
    {
        Project project = Project.builder().id(1L).startDate(LocalDate.of(2030, 1, 7)).build();
        Stage predecessor = new Stage("Previous", project);
        Stage next = new Stage("Next", project);
        next.getPredecessors().add(predecessor);
        new Task(predecessor, "Preparation", 2);
        Task work = new Task(next, "Work", 1);
        assertEquals(LocalDate.of(2030, 1, 9), WorkingDaySchedule.calculateTaskDates(work, project.getStartDate()).startDate());
    }
}
