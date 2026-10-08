package app.employee.domain;

import app.employee.presentation.dto.EmployeeCapacityDTO;
import app.employee.presentation.dto.EmployeeCompetencesUpdateDTO;
import app.employee.presentation.dto.EmployeeCreateDTO;
import app.employee.presentation.dto.EmployeeDTO;
import app.employee.presentation.dto.EmployeeUpdateDTO;

import java.util.List;

public interface IEmployeeService
{
    EmployeeDTO create(EmployeeCreateDTO dto);

    EmployeeDTO get(Long id);

    List<EmployeeDTO> getAll();

    EmployeeDTO update(Long id, EmployeeUpdateDTO dto);

    EmployeeDTO updateCompetences(Long id, EmployeeCompetencesUpdateDTO dto);

    EmployeeCapacityDTO calculateDailyCapacity(Long competenceId);

    void delete(Long id);

    void setActive(Long id, boolean active);
}