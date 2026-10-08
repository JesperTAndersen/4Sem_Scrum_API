package app.employee.presentation.dto;

import java.util.List;

public record EmployeeCompetencesUpdateDTO(List<Long> competenceIds)
{
}