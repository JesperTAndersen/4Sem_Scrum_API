package app.workload.domain;

import app.workload.presentation.dto.OverAllocationResultDTO;

import java.time.LocalDate;

public interface IEmployeeWorkloadService
{
    OverAllocationResultDTO findConflicts(LocalDate startDate, LocalDate endDate);
}
