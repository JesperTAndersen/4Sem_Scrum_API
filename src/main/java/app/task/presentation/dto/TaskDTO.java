package app.task.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.competence.presentation.dto.SlimCompetenceDTO;
import app.employee.presentation.dto.EmployeeReferenceDTO;
import app.task.domain.Task.StaffingStatus;
import app.task.domain.Task.TaskStatus;

public record TaskDTO(
        Long id,
        String name,
        int minimumDurationInDays,
        SlimCompetenceDTO competence,
        double estimate,
        double bestEstimate,
        double likelyEstimate,
        double worstEstimate,
        BigDecimal cost,
        LocalDate startDate,
        LocalDate endDate,
        Double laborDurationInDays,
        Double scheduledDurationInDays,
        TaskStatus status,
        StaffingStatus staffingStatus,
        List<Long> predecessorIds,
        Double dependencyStartOffsetInDays,
        Double dependencyFinishOffsetInDays,
        List<EmployeeReferenceDTO> assignedEmployees,
        List<StaffingWarning> staffingWarnings
)
{
}
