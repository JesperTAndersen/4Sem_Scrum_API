package app.employee.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

import app.competence.domain.Competence;

public record EmployeeDTO(
        Long id,
        String firstName,
        String lastName,
        double dailyCapacity,
        List<Competence> competences,
        LocalDateTime createdAt,
        LocalDateTime updateAt)
{
}