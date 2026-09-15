package app.mappers;

import app.DTOs.project.CreateProjectDTO;
import app.DTOs.project.ProjectDTO;
import app.DTOs.project.UpdateProjectDTO;
import app.entities.Project;
import app.enums.ProjectStatus;

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
                project.getCreatedBy(),
                project.getCreatedAt(),
                project.getUpdatedBy(),
                project.getUpdatedAt()
        );
    }

    public static Project toEntity(CreateProjectDTO dto, ProjectStatus status, String createdBy)
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

    public static Project toEntity(UpdateProjectDTO dto, Project existingProject, String updatedBy)
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
