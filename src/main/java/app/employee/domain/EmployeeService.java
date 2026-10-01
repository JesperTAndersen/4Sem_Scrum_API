package app.employee.domain;

import app.employee.data.EmployeeMapper;
import app.employee.data.IEmployeeDAO;
import app.employee.presentation.dto.EmployeeCreateDTO;
import app.employee.presentation.dto.EmployeeDTO;
import app.employee.presentation.dto.EmployeeUpdateDTO;
import app.exceptions.BadRequestException;
import app.utils.ValidationUtil;

import java.util.List;

public class EmployeeService implements IEmployeeService
{
    private final IEmployeeDAO employeeDAO;

    public EmployeeService(IEmployeeDAO employeeDAO)
    {
        this.employeeDAO = employeeDAO;
    }

    @Override
    public EmployeeDTO create(EmployeeCreateDTO dto)
    {
        validate(dto == null ? null : dto.firstName(),
                dto == null ? null : dto.lastName(),
                dto == null ? null : dto.dailyCapacity());

        Employee created = employeeDAO.create(EmployeeMapper.toEntity(dto));
        return EmployeeMapper.toDTO(created);
    }

    @Override
    public EmployeeDTO get(Long id)
    {
        ValidationUtil.validateId(id);
        return EmployeeMapper.toDTO(employeeDAO.get(id));
    }

    @Override
    public List<EmployeeDTO> getAll()
    {
        return employeeDAO.getAll().stream()
                .map(EmployeeMapper::toDTO)
                .toList();
    }

    @Override
    public EmployeeDTO update(Long id, EmployeeUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        validate(dto == null ? null : dto.firstName(),
                dto == null ? null : dto.lastName(),
                dto == null ? null : dto.dailyCapacity());

        Employee employee = employeeDAO.get(id);
        employee.update(dto.firstName().trim(), dto.lastName().trim(), dto.dailyCapacity());
        return EmployeeMapper.toDTO(employeeDAO.update(employee));
    }

    @Override
    public void delete(Long id)
    {
        ValidationUtil.validateId(id);
        employeeDAO.delete(id);
    }

    @Override
    public void setActive(Long id, boolean active)
    {
        ValidationUtil.validateId(id);
        employeeDAO.setActive(id, active);
    }

    private void validate(String firstName, String lastName, Double dailyCapacity)
    {
        if (firstName == null || firstName.isBlank())
        {
            throw new BadRequestException("First name is required");
        }
        if (lastName == null || lastName.isBlank())
        {
            throw new BadRequestException("Last name is required");
        }
        if (dailyCapacity == null || !Double.isFinite(dailyCapacity))
        {
            throw new BadRequestException("Daily capacity must be a finite positive number");
        }

        ValidationUtil.validateName(firstName, "First name");
        ValidationUtil.validateName(lastName, "Last name");
        ValidationUtil.validatePositive(dailyCapacity, "Daily capacity");
    }
}
