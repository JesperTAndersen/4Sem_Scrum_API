package app.task.presentation.dto;

public record TaskCreateDTO(
        Long stageId,
        String name,
        Long competenceId,
        Double bestEstimate,
        Double likelyEstimate,
        Double worstEstimate,
        Integer minimumDurationInDays)
{
}