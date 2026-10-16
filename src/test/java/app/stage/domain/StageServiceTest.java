package app.stage.domain;

import app.persistence.testdoubles.InMemoryProjectDAO;
import app.persistence.testdoubles.InMemoryStageDAO;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.stage.presentation.dto.StageCreateDTO;
import app.stage.presentation.dto.StageDTO;
import app.stage.presentation.dto.StageUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import app.exceptions.BadRequestException;
import app.task.domain.Task;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StageServiceTest
{
    private InMemoryStageDAO stageDAO;
    private StageService stageService;

    @BeforeEach
    void setUp()
    {
        InMemoryProjectDAO projectDAO = new InMemoryProjectDAO();
        Project project = Project.builder()
                .id(1L)
                .title("Website redesign")
                .startDate(LocalDate.of(2030, 1, 7))
                .deadline(LocalDate.of(2030, 6, 28))
                .status(ProjectStatus.DRAFT)
                .build();
        projectDAO.seed(project);

        stageDAO = new InMemoryStageDAO();
        stageDAO.create(new Stage("Planning", project));
        stageService = new StageService(stageDAO, projectDAO);
    }


    @Test
    @DisplayName("Stage dependencies accept multiple predecessors but reject duplicates and indirect cycles")
    void stageDependencyValidation()
    {
        Stage planning = stageDAO.get(1L);
        Stage implementation = stageDAO.create(new Stage("Implementation", planning.getProject()));
        Stage qa = stageDAO.create(new Stage("QA", planning.getProject()));
        stageService.addPredecessor(implementation.getId(), planning.getId());
        stageService.addPredecessor(qa.getId(), implementation.getId());
        assertEquals(java.util.List.of(implementation.getId()), stageService.get(qa.getId()).predecessorIds());
        assertThrows(BadRequestException.class, () -> stageService.addPredecessor(implementation.getId(), planning.getId()));
        assertThrows(BadRequestException.class, () -> stageService.addPredecessor(planning.getId(), qa.getId()));
        assertThrows(BadRequestException.class, () -> stageService.addPredecessor(planning.getId(), planning.getId()));
        assertThrows(BadRequestException.class, () -> stageService.delete(planning.getId()));
        stageService.removePredecessor(qa.getId(), implementation.getId());
        assertTrue(stageService.get(qa.getId()).predecessorIds().isEmpty());
    }

    @Test
    @DisplayName("Stage cannot depend on another project's stage, nor be delayed after work starts")
    void stageDependencyRequiresSameProjectAndNotStarted()
    {
        Stage planning = stageDAO.get(1L);
        Stage other = stageDAO.create(new Stage("Other", Project.builder().id(2L)
                .startDate(LocalDate.of(2030, 1, 7)).deadline(LocalDate.of(2030, 6, 28)).build()));
        assertThrows(BadRequestException.class, () -> stageService.addPredecessor(1L, other.getId()));
        Stage second = stageDAO.create(new Stage("Second", planning.getProject()));
        Task active = new Task(second, "Started", 1);
        active.changeStatus(Task.TaskStatus.IN_PROGRESS);
        assertThrows(BadRequestException.class, () -> stageService.addPredecessor(second.getId(), 1L));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/stage/stage-service/create-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - validates the project and the name")
    void createValidatesFields(Long projectId, String name, String expected, String reason)
    {
        StageCreateDTO dto = new StageCreateDTO(projectId, name);

        if (expected.equals("ACCEPT"))
        {
            StageDTO result = stageService.create(dto);
            assertThat(result.name(), is(name.trim()));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> stageService.create(dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/stage/stage-service/update-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - validates the stage and the name")
    void updateValidatesFields(Long id, String name, String expected, String reason)
    {
        StageUpdateDTO dto = new StageUpdateDTO(name);

        if (expected.equals("ACCEPT"))
        {
            stageService.update(id, dto);
            assertThat(stageService.get(id).name(), is(name.trim()));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> stageService.update(id, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/stage/stage-service/get-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get - requires an existing stage")
    void getRequiresExistingStage(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            assertThat(stageService.get(id).id(), is(id));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> stageService.get(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/stage/stage-service/delete-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - requires an existing stage")
    void deleteRequiresExistingStage(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            stageService.delete(id);
            assertThat(stageDAO.getStored(id), nullValue());
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> stageService.delete(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }
}
