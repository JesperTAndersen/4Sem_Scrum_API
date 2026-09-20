package app.project.data;

import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.SlimProjectDTO;
import app.stage.domain.Stage;
import app.task.domain.Task;
import app.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class ProjectMapperTest
{
    @Test
    @DisplayName("Map project - should include its stages and tasks")
    void mapProjectIncludesStagesAndTasks()
    {
        Project project = project();
        Stage planning = new Stage("Planning", project);
        Stage delivery = new Stage("Delivery", project);
        project.getStages().add(planning);
        project.getStages().add(delivery);

        new Task(planning, "Requirements", 4);
        Task completedTask = new Task(planning, "Design", 8);
        completedTask.changeStatus(Task.TaskStatus.DONE);
        new Task(delivery, "Implementation", 16);

        ProjectDTO result = ProjectMapper.toDTO(project);

        assertThat(result.stages(), hasSize(2));
        assertThat(result.stages().stream().map(stage -> stage.tasks().size()).sorted().toList(), contains(1, 2));
        assertThat(result.stages().stream()
                .flatMap(stage -> stage.tasks().stream())
                .filter(task -> task.status() == Task.TaskStatus.DONE)
                .count(), is(1L));
    }

    @Test
    @DisplayName("Map slim project - should include total and completed task counts")
    void mapSlimProjectIncludesTaskCounts()
    {
        Project project = project();
        Stage stage = new Stage("Planning", project);
        project.getStages().add(stage);

        new Task(stage, "Requirements", 4);
        Task completedTask = new Task(stage, "Design", 8);
        completedTask.changeStatus(Task.TaskStatus.DONE);

        SlimProjectDTO result = ProjectMapper.toSlimProjectDTO(project);

        assertThat(result.taskCountDTO().totalTaskCount(), is(2));
        assertThat(result.taskCountDTO().taskFinished(), is(1));
    }

    private Project project()
    {
        User user = new User("Test", "Manager", "test@example.com", "hashed-password");

        return Project.builder()
                .id(1L)
                .title("Test project")
                .description("Test description")
                .startDate(LocalDate.of(2026, 1, 1))
                .deadline(LocalDate.of(2026, 12, 31))
                .status(ProjectStatus.DRAFT)
                .createdBy(user)
                .updatedBy(user)
                .stages(new HashSet<>())
                .build();
    }
}
