package app.dashboard.presentation.dto;

import app.project.domain.Project;
import app.project.presentation.dto.ScheduleDTO;

public record ScheduledProject(
        Project project,
        ScheduleDTO scheduleDTO
)
{
}
