package app.task.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import app.config.HibernateTestConfig;
import app.exceptions.DatabaseException;
import app.persistence.testutils.TestPopulator;
import app.stage.data.StageDAO;
import app.stage.domain.Stage;
import app.task.domain.Task;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class TaskDAOTest
{
    private final EntityManagerFactory emf = HibernateTestConfig.getEntityManagerFactory();
    private TaskDAO taskDAO;
    private Stage stage;

    @BeforeEach
    void setUp()
    {
        var projects = TestPopulator.populateProjects(emf);
        stage = new StageDAO(emf).create(new Stage("Snapshot stage", projects.get("project1")));
        taskDAO = new TaskDAO(emf);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/task/task-dao/planning-snapshots.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Completion persistence - status and all snapshot fields survive a fresh entity manager")
    void completionSnapshotSurvivesReload(LocalDate start, LocalDate end, Double labor, Double scheduled, String reason)
    {
        Task task = taskDAO.create(new Task(stage, "Completed task", 0));
        task.recordPlanningSnapshot(start, end, labor, scheduled);
        task.changeStatus(Task.TaskStatus.DONE);
        taskDAO.update(task);

        // Every DAO read creates a fresh EntityManager; this cannot read the original object's fields.
        Task reloaded = taskDAO.get(task.getId());
        assertThat(reloaded.getStatus(), is(Task.TaskStatus.DONE));
        assertThat(reloaded.hasPlanningSnapshot(), is(true));
        assertThat(reloaded.getPlanningSnapshotRecordedAt(), notNullValue());
        assertThat(reloaded.getCompletedStartDate(), is(start));
        assertThat(reloaded.getCompletedEndDate(), is(end));
        assertThat(reloaded.getCompletedLaborDurationInDays(), is(labor));
        assertThat(reloaded.getCompletedScheduledDurationInDays(), is(scheduled));
        assertThat(reloaded.getLaborDurationInDays(), is(labor));
        assertThat(reloaded.getScheduledDurationInDays(), is(scheduled));
    }

    @Test
    @DisplayName("Failed completion persistence - snapshot and DONE status roll back together")
    void failedSaveDoesNotCommitStatusOrSnapshot()
    {
        Task task = taskDAO.create(new Task(stage, "Unfinished task", 0));
        task.recordPlanningSnapshot(LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 11), 4.0, 5.0);
        task.changeStatus(Task.TaskStatus.DONE);
        // Exceed the database's varchar(255) name limit to fail the same transaction at flush.
        task.update("x".repeat(256), null);

        assertThrows(DatabaseException.class, () -> taskDAO.update(task));

        Task reloaded = taskDAO.get(task.getId());
        assertThat(reloaded.getName(), is("Unfinished task"));
        assertThat(reloaded.getStatus(), is(Task.TaskStatus.NOT_STARTED));
        assertThat(reloaded.hasPlanningSnapshot(), is(false));
        assertThat(reloaded.getCompletedStartDate(), nullValue());
        assertThat(reloaded.getCompletedEndDate(), nullValue());
        assertThat(reloaded.getCompletedLaborDurationInDays(), nullValue());
        assertThat(reloaded.getCompletedScheduledDurationInDays(), nullValue());
    }
}
