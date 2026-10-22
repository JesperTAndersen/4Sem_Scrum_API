package app.workload.presentation.dto;

import java.time.LocalDate;
import java.util.List;

public record OverAllocationResultDTO(
        LocalDate startDate,
        LocalDate endDate,
        List<EmployeeOverAllocationDTO> conflicts,
        List<Long> unassessedTaskIds,
        List<UnassessedTaskDTO> unassessedTasks
)
{}
