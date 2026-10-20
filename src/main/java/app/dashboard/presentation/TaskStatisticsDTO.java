package app.dashboard.presentation;

public record TaskStatisticsDTO(
        int notStarted,
        int inProgress,
        int done
)
{
}
