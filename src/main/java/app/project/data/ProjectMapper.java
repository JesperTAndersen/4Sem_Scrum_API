package app.project.data;

import app.project.presentation.dto.CreateProjectDTO;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.SlimProjectDTO;
import app.project.presentation.dto.UpdateProjectDTO;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.user.data.UserMapper;
import app.user.domain.User;
import app.stage.data.StageMapper;
import app.task.presentation.dto.TaskCountDTO;
import app.task.domain.Task;

public class ProjectMapper
{
    private ProjectMapper()
    {
    }

    public static ProjectDTO toDTO(Project project)
    {
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
                project.getStages().stream().map(StageMapper::toDTO).toList()
        );
    }

    public static SlimProjectDTO toSlimProjectDTO(Project project)
    {
        int total = project.getStages().stream()
                .mapToInt(stage -> stage.getTasks().size())
                .sum();
        int finished = project.getStages().stream()
                .flatMap(stage -> stage.getTasks().stream())
                .map(Task::getStatus)
                .filter(Task.TaskStatus.DONE::equals)
                .toList()
                .size();

        return new SlimProjectDTO(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                UserMapper.toReferenceDTO(project.getCreatedBy()),
                project.getStartDate(),
                project.getDeadline(),
                new TaskCountDTO(total, finished),
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
}
