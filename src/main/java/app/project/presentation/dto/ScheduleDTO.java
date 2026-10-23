package app.project.presentation.dto;

import java.time.LocalDate;
import java.util.List;

import app.task.presentation.dto.StaffingWarning;

public record ScheduleDTO(
        LocalDate calculatedFinishDate,
        Boolean feasible,
        List<StaffingWarning> staffingWarnings
)
{
}
