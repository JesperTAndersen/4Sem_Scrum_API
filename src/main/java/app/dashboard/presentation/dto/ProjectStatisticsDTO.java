package app.dashboard.presentation.dto;

import app.project.domain.ProjectStatus;

import java.util.Map;

public record ProjectStatisticsDTO(
        int total,
        Map<ProjectStatus, Integer> byStatus,
        int infeasible
)
{
}
