package app.employee.presentation.dto;

public record EmployeeReferenceDTO(
        Long id,
        String firstName,
        String lastName,
        boolean active,
        double dailyCapacity
)
{
}
