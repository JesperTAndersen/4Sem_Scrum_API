package app.dtos.task;

public record TaskCreateDTO(
        Long stageId,
        String name,
        float estimate,
        float duration,
        int crewSize)
{
}
