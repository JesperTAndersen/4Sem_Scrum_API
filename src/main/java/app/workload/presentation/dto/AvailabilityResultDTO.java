package app.workload.presentation.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResultDTO(
        LocalDate date,
        List<EmployeeAvailabilityDTO> employees,
        List<Long> unassessedTaskIds)
{}