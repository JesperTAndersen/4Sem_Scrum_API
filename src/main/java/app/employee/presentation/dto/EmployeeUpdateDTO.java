package app.employee.presentation.dto;

public record EmployeeUpdateDTO(
        String firstName,
        String lastName,
        Double dailyCapacity,
        Boolean standardCapacity
)
{
}