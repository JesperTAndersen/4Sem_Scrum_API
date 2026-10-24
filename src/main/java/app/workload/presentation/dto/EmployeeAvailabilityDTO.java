package app.workload.presentation.dto;

import app.competence.presentation.dto.SlimCompetenceDTO;

import java.util.List;

public record EmployeeAvailabilityDTO(
        Long employeeId,
        String firstName,
        String lastName,
        double dailyCapacity,
        double allocatedHours,
        double remainingHours,
        List<SlimCompetenceDTO> competences)
{}