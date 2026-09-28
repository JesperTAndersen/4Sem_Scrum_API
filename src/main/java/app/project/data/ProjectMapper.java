package app.project.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.project.presentation.dto.CreateProjectDTO;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.SlimProjectDTO;
import app.project.presentation.dto.UpdateProjectDTO;
import app.stage.data.StageMapper;
import app.stage.domain.Stage;
import app.stage.presentation.dto.StageDTO;
import app.task.domain.Task;
import app.task.presentation.dto.TaskCountDTO;
import app.user.data.UserMapper;
import app.user.domain.User;

public class ProjectMapper
{
    private ProjectMapper()
    {
    }

    public static ProjectDTO toDTO(Project project)
    {
        TaskCountDTO taskCount = buildTaskCountDTO(project);

        // TODO: put endDate in projectDTO
        LocalDate endDate = project.getStartDate();
        Set<Stage> stages = project.getStages();
        List<StageDTO> stageDTOs = new ArrayList<>(stages.size());
        for (Stage stage : stages) {
            StageDTO dto = StageMapper.toDTO(stage, project.getStartDate());
            stageDTOs.add(dto);
            if (dto.endDate().isAfter(endDate)) {
                endDate = dto.startDate();
            }
        }

        return new ProjectDTO(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStartDate(),
                project.getDeadline(),
                project.getStatus(),
                UserMapper.toReferenceDTO(project.getCreatedBy()),
                project.getCreatedAt(),
                UserMapper.toReferenceDTO(project.getUpdatedBy()),
                project.getUpdatedAt(),
                project.getTotalEstimatedHours(),
                project.getTotalCost(),
                stageDTOs,
                taskCount
        );
    }

    public static SlimProjectDTO toSlimProjectDTO(Project project)
    {
        TaskCountDTO taskCount = buildTaskCountDTO(project);

        return new SlimProjectDTO(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                UserMapper.toReferenceDTO(project.getCreatedBy()),
                project.getStartDate(),
                project.getDeadline(),
                taskCount,
                project.getStatus()

        );
    }

    public static Project toEntity(CreateProjectDTO dto, ProjectStatus status, User createdBy)
    {
        return Project.builder()
                .title(dto.title())
                .description(dto.description())
                .startDate(dto.startDate())
                .deadline(dto.deadline())
                .status(status)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();
    }

    public static Project toEntity(UpdateProjectDTO dto, Project existingProject, User updatedBy)
    {
        return Project.builder()
                .id(existingProject.getId())
                .title(dto.title())
                .description(dto.description())
                .startDate(dto.startDate())
                .deadline(dto.deadline())
                .status(dto.status())
                .createdBy(existingProject.getCreatedBy())
                .createdAt(existingProject.getCreatedAt())
                .updatedBy(updatedBy)
                .updatedAt(existingProject.getUpdatedAt())
                .build();
    }

    private static int totalNumOfTasks(Project project)
    {
        return project.getStages().stream()
                .mapToInt(stage -> stage.getTasks().size())
                .sum();
    }

    private static int numOfNotStartedTasks(Project project)
    {
        return numOfTasksByStatus(project, Task.TaskStatus.NOT_STARTED);
    }

    private static int numOfInProgressTasks(Project project)
    {
        return numOfTasksByStatus(project, Task.TaskStatus.IN_PROGRESS);
    }

    private static int numOfDoneTasks(Project project){
        return numOfTasksByStatus(project, Task.TaskStatus.DONE);
    }

    private static int numOfTasksByStatus(Project project, Task.TaskStatus status)
    {
        return project.getStages().stream()
                .flatMap(stage -> stage.getTasks().stream())
                .map(Task::getStatus)
                .filter(status::equals)
                .toList()
                .size();
    }

    private static TaskCountDTO buildTaskCountDTO(Project project)
    {
        int totalNumOfTasks = totalNumOfTasks(project);
        int notStartedTasks = numOfNotStartedTasks(project);
        int inProgressTasks = numOfInProgressTasks(project);
        int doneTasks = numOfDoneTasks(project);

        return new TaskCountDTO(totalNumOfTasks, notStartedTasks, inProgressTasks, doneTasks);
    }
}