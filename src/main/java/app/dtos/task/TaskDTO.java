package app.dtos.task;

public record TaskDTO(
        Long id,
        String name,
        float estimate,
        float duration,
        int crewSize)
{
}
