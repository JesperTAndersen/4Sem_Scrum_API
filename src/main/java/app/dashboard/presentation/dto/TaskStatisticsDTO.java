package app.dashboard.presentation.dto;

public record TaskStatisticsDTO(
        int notStarted,
        int inProgress,
        int done
)
{
}
