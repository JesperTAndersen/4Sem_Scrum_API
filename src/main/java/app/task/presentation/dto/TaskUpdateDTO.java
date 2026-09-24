package app.task.presentation.dto;

public record TaskUpdateDTO(
        String name,
        Integer minimumDurationInDays,
        Long competenceId,
        Double estimate)
{
}
