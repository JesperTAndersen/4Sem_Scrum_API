package app.employee.presentation.dto;

import java.util.List;

import app.competence.domain.Competence;

public record EmployeeDTO(
        String firstName,
        String lastName,
        double dailyCapacity,
        List<Competence> competences)
{
}