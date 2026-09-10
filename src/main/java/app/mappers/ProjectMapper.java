package app.mappers;

import app.DTOs.project.ProjectDTO;
import app.entities.Project;

public class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectDTO toDTO(Project project) {
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

    public static Project toEntity(ProjectDTO dto) {
        return Project.builder()
                .id(dto.id())
                .title(dto.title())
                .description(dto.description())
                .startDate(dto.startDate())
                .deadline(dto.deadline())
                .status(dto.status())
                .createdBy(dto.createdBy())
                .createdAt(dto.createdAt())
                .updatedBy(dto.updatedBy())
                .updatedAt(dto.updatedAt())
                .build();
    }
}