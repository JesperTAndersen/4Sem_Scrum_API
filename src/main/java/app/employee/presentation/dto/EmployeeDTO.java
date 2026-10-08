package app.employee.presentation.dto;

import app.competence.presentation.dto.CompetenceDTO;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeDTO(
        Long id,
        String firstName,
        String lastName,
        double dailyCapacity,
        boolean standardCapacity,
        boolean active,
        List<CompetenceDTO> competences,
        LocalDateTime createdAt,
        LocalDateTime updateAt)
{
}