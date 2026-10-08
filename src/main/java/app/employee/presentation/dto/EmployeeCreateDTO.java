package app.employee.presentation.dto;

public record EmployeeCreateDTO(
        String firstName,
        String lastName,
        Double dailyCapacity,
        Boolean standardCapacity
)
{
}