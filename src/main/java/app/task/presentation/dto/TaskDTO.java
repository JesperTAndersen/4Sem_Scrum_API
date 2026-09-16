package app.task.presentation.dto;

public record TaskDTO(
        Long id,
        String name,
        float estimate,
        float duration,
        int crewSize)
{
}
