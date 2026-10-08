package app.task.domain;

import app.competence.domain.Competence;
import app.exceptions.NotFoundException;
import app.persistence.testdoubles.EntityIds;
import app.persistence.testdoubles.InMemoryCompetenceDAO;
import app.persistence.testdoubles.InMemoryStageDAO;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.stage.domain.Stage;
import app.task.data.TaskDAO;
import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskServiceTest
{
    private static final double TOLERANCE = 1e-9;

    private InMemoryTaskDAO taskDAO;
    private TaskService taskService;

    /**
     * Seeds tasks 1, 2 and 3 in project 1 and task 4 in project 2. Task 2 depends on task 1, and task 3 on task 2.
     * Competence 1 is active, competence 2 is inactive.
     */
    @BeforeEach
    void setUp()
    {
        InMemoryStageDAO stageDAO = new InMemoryStageDAO();
        Stage planning = stageDAO.create(new Stage("Planning", project(1L)));
        Stage otherProjectStage = stageDAO.create(new Stage("Other project", project(2L)));

        InMemoryCompetenceDAO competenceDAO = new InMemoryCompetenceDAO();
        Competence carpenter = competenceDAO.create(new Competence("carpenter", new BigDecimal("850.00")));
        competenceDAO.create(new Competence("plumber", new BigDecimal("800.00"))).setActive(false);

        taskDAO = new InMemoryTaskDAO();
        Task requirements = taskDAO.create(new Task(planning, "Requirements", 1));
        requirements.setCompetence(carpenter, 8.0);
        Task design = taskDAO.create(new Task(planning, "Design", 1));
        Task build = taskDAO.create(new Task(planning, "Build", 1));
        taskDAO.create(new Task(otherProjectStage, "Other project task", 1));
        design.addPredecessor(requirements);
        build.addPredecessor(design);

        taskService = new TaskService(taskDAO, stageDAO, competenceDAO);
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvFileSource(resources = "/testcases/task/task-service/create-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - validates the stage, name, competence, estimate and minimum duration")
    void createValidatesFields(Long stageId, String name, Long competenceId, Double estimate,
                               Integer minimumDurationInDays, String expected, String reason)
    {
        TaskCreateDTO dto = new TaskCreateDTO(stageId, name, competenceId, estimate, minimumDurationInDays);

        if (expected.equals("ACCEPT"))
        {
            TaskDTO result = taskService.create(dto);
            assertThat(result.name(), is(name.trim()));
            assertThat(result.status(), is(Task.TaskStatus.NOT_STARTED));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.create(dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {7}")
    @CsvFileSource(resources = "/testcases/task/task-service/update-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - validates supplied fields and the competence and estimate combination")
    void updateValidatesFields(Long id, String name, Integer minimumDurationInDays, Long competenceId, Double estimate,
                               Task.TaskStatus status, String expected, String reason)
    {
        TaskUpdateDTO dto = new TaskUpdateDTO(name, minimumDurationInDays, competenceId, estimate, status);

        if (expected.equals("ACCEPT"))
        {
            TaskDTO before = taskService.get(id);
            taskService.update(id, dto);
            TaskDTO stored = taskService.get(id);

            assertThat(stored.name(), is(name == null ? before.name() : name.trim()));
            assertThat(stored.minimumDurationInDays(),
                    is(minimumDurationInDays == null ? before.minimumDurationInDays() : minimumDurationInDays));
            assertThat(stored.status(), is(status == null ? before.status() : status));
            if (competenceId == null)
            {
                assertThat(stored.competence(), is(before.competence()));
                assertThat(stored.estimate(), is(before.estimate()));
            }
            else if (competenceId == 0)
            {
                assertThat(stored.competence(), nullValue());
                assertThat(stored.estimate(), is(0.0));
            }
            else
            {
                assertThat(stored.competence().id(), is(competenceId));
                assertThat(stored.estimate(), is(estimate));
            }
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.update(id, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/add-predecessor.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Add predecessor - accepts only a new, acyclic dependency within one project")
    void addPredecessorValidatesDependency(Long taskId, Long predecessorId, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            taskService.addPredecessor(taskId, predecessorId);
            assertThat(taskService.get(taskId).predecessorIds(), hasItem(predecessorId));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.addPredecessor(taskId, predecessorId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/remove-predecessor.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Remove predecessor - requires an existing dependency")
    void removePredecessorRequiresExistingDependency(Long taskId, Long predecessorId, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            taskService.removePredecessor(taskId, predecessorId);
            assertThat(taskService.get(taskId).predecessorIds(), not(hasItem(predecessorId)));
        }
        else
        {
            Exception exception = assertThrows(Exception.class,
                    () -> taskService.removePredecessor(taskId, predecessorId));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/get-scheduled-duration.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Scheduled duration - is the greater of the labor duration and the minimum duration")
    void calculatesScheduledDuration(double estimate, int minimumDurationInDays, double expected, String reason)
    {
        TaskDTO created = taskService.create(new TaskCreateDTO(1L, "Duration task", 1L, estimate, minimumDurationInDays));

        assertThat(taskService.get(created.id()).scheduledDurationInDays(), closeTo(expected, TOLERANCE));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/task/task-service/get-dependency-offsets.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Dependency offset - a task starts when its latest predecessor finishes")
    void calculatesDependencyOffsets(Integer firstPredecessorDays, Integer secondPredecessorDays, double expected,
                                     String reason)
    {
        Long taskId = createTask("Dependent task", 1);
        for (Integer predecessorDays : new Integer[] { firstPredecessorDays, secondPredecessorDays })
        {
            if (predecessorDays != null)
            {
                taskService.addPredecessor(taskId, createTask("Predecessor", predecessorDays));
            }
        }

        assertThat(taskService.get(taskId).dependencyStartOffsetInDays(), closeTo(expected, TOLERANCE));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/task/task-service/get-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get - requires an existing task")
    void getRequiresExistingTask(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            assertThat(taskService.get(id).id(), is(id));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.get(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/task/task-service/delete-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - requires an existing task")
    void deleteRequiresExistingTask(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            taskService.delete(id);
            assertThat(taskDAO.getStored(id), nullValue());
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> taskService.delete(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    private Long createTask(String name, int minimumDurationInDays)
    {
        return taskService.create(new TaskCreateDTO(1L, name, 1L, 0.0, minimumDurationInDays)).id();
    }

    private static Project project(Long id)
    {
        return Project.builder()
                .id(id)
                .title("Project " + id)
                .startDate(LocalDate.of(2030, 1, 7))
                .deadline(LocalDate.of(2030, 6, 28))
                .status(ProjectStatus.DRAFT)
                .build();
    }

    /**
     * {@code TaskService} depends on the concrete {@code TaskDAO}, so this double extends it instead of implementing an
     * interface. It mirrors {@code TaskDAO}: a missing task raises {@link NotFoundException}, and deleting a task also
     * removes it as a predecessor of other tasks.
     */
    private static final class InMemoryTaskDAO extends TaskDAO
    {
        private final Map<Long, Task> tasks = new LinkedHashMap<>();
        private long nextId = 1L;

        private InMemoryTaskDAO()
        {
            super(null);
        }

        @Override
        public Task create(Task task)
        {
            EntityIds.assign(task, nextId++);
            tasks.put(task.getId(), task);
            return task;
        }

        @Override
        public Task get(Long id)
        {
            Task task = tasks.get(id);
            if (task == null)
            {
                throw new NotFoundException("No task found with id: " + id);
            }
            return task;
        }

        @Override
        public List<Task> getAll()
        {
            return new ArrayList<>(tasks.values());
        }

        @Override
        public Task update(Task task)
        {
            get(task.getId());
            tasks.put(task.getId(), task);
            return task;
        }

        @Override
        public boolean delete(Long id)
        {
            Task deleted = get(id);
            tasks.values().forEach(task -> task.removePredecessor(deleted));
            return tasks.remove(id) != null;
        }

        private Task getStored(Long id)
        {
            return tasks.get(id);
        }
    }
}
