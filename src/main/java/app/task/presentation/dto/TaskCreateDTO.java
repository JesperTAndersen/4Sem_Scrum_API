package app.task.presentation.dto;

public record TaskCreateDTO(
        Long stageId,
        String name,
        float estimate,
        float duration,
        int crewSize)
{
}
