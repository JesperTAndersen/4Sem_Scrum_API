package app.workload.domain;

import app.workload.presentation.dto.OverAllocationResultDTO;
import app.workload.presentation.dto.AvailabilityResultDTO;

import java.time.LocalDate;

public interface IEmployeeWorkloadService
{
    AvailabilityResultDTO findAvailability(LocalDate date, Long competenceId);

    OverAllocationResultDTO findConflicts(LocalDate startDate, LocalDate endDate);
}
