package app.employee.data;

import app.competence.data.CompetenceMapper;
import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.employee.presentation.dto.EmployeeCreateDTO;
import app.employee.presentation.dto.EmployeeDTO;

import java.util.Comparator;

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
                employee.isStandardCapacity(),
                employee.isActive(),
                employee.getCompetences().stream()
                        .sorted(Comparator.comparing(Competence::getId))
                        .map(CompetenceMapper::toDTO)
                        .toList(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    public static Employee toEntity(EmployeeCreateDTO dto)
    {
        return new Employee(
                dto.firstName().trim(),
                dto.lastName().trim(),
                dto.dailyCapacity() == null ? 0.0 : dto.dailyCapacity());
    }
}