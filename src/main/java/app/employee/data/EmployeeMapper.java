package app.employee.data;

import app.employee.domain.Employee;
import app.employee.presentation.dto.EmployeeCreateDTO;
import app.employee.presentation.dto.EmployeeDTO;

public class EmployeeMapper
{
    public static EmployeeDTO toDTO(Employee employee)
    {
        if (employee == null)
        {
            return null;
        }

        return new EmployeeDTO(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getDailyCapacity(),
                employee.getCompetences(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    public static Employee toEntity(EmployeeCreateDTO dto)
    {
        return new Employee(
                dto.firstName().trim(),
                dto.lastName().trim(),
                dto.dailyCapacity());
    }
}
