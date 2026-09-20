package app.task.presentation.dto;

import app.competence.presentation.dto.CompetenceDTO;
import app.task.domain.Task.TaskStatus;

import java.util.List;

public record TaskDTO(
        Long id,
        String name,
        double estimate,
        TaskStatus status,
        List<CompetenceDTO> competences)
{
}
