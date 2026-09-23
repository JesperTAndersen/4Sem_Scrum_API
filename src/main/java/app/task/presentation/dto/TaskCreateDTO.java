package app.task.presentation.dto;

public record TaskCreateDTO(
        Long stageId,
        String name,
        Long competenceId,
        Integer minimumDurationInDays)
{
}
