package app.employee.presentation.dto;

public record EmployeeCreateDTO(
        String firstName,
        String lastName,
        Double dailyCapacity
        //TODO: standardCapacity boolean, that sets wether an employee have standard 7.5 hours or an independant work capacity
)
{
}