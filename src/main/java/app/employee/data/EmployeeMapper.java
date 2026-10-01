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

        );
    }

    public static Employee toEntity(EmployeeCreateDTO dto)
    {
        return new Employee();
    }
}
