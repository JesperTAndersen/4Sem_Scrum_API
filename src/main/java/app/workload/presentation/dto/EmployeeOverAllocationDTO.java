package app.workload.presentation.dto;

import java.time.LocalDate;
import java.util.List;

public record EmployeeOverAllocationDTO(
        Long employeeId,
        String firstName,
        String lastName,
        LocalDate date,
        double availableHours,
        double allocatedHours,
        double excessHours,
        List<TaskHoursDTO> taskHours
)
{}