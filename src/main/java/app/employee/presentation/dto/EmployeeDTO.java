package app.employee.presentation.dto;

import app.competence.domain.Competence;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeDTO(
        Long id,
        String firstName,
        String lastName,
        double dailyCapacity,
        boolean standardCapacity,
        List<Competence> competences,
        LocalDateTime createdAt,
        LocalDateTime updateAt)
{
}