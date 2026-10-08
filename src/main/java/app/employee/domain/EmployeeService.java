package app.employee.domain;

import app.capacity.data.ICompanyCapacityDAO;
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
    private final ICompanyCapacityDAO companyCapacityDAO;

    public EmployeeService(IEmployeeDAO employeeDAO, ICompanyCapacityDAO companyCapacityDAO)
    {
        this.employeeDAO = employeeDAO;
        this.companyCapacityDAO = companyCapacityDAO;
    }

    @Override
    public EmployeeDTO create(EmployeeCreateDTO dto)
    {
        if (dto == null) throw new BadRequestException("Employee payload is required");
        validate(dto.firstName(), dto.lastName(), dto.dailyCapacity(),
                Boolean.TRUE.equals(dto.standardCapacity()));

        Employee employee = EmployeeMapper.toEntity(dto);
        employee.configureCapacity(Boolean.TRUE.equals(dto.standardCapacity()), companyCapacityDAO.get());
        Employee created = employeeDAO.create(employee);
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
        if (dto == null) throw new BadRequestException("Employee payload is required");
        Employee employee = employeeDAO.get(id);
        boolean standardCapacity = dto.standardCapacity() == null
                ? employee.isStandardCapacity()
                : dto.standardCapacity();
        validate(dto.firstName(), dto.lastName(), dto.dailyCapacity(), standardCapacity);
        employee.update(dto.firstName().trim(), dto.lastName().trim(),
                standardCapacity ? 0.0 : dto.dailyCapacity());
        employee.configureCapacity(standardCapacity, companyCapacityDAO.get());
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

    private void validate(String firstName, String lastName, Double dailyCapacity, boolean standardCapacity)
    {
        if (firstName == null || firstName.isBlank())
        {
            throw new BadRequestException("First name is required");
        }
        if (lastName == null || lastName.isBlank())
        {
            throw new BadRequestException("Last name is required");
        }
        if (!standardCapacity && (dailyCapacity == null || !Double.isFinite(dailyCapacity) || dailyCapacity <= 0))
        {
            throw new BadRequestException("Daily capacity must be a finite positive number");
        }

        ValidationUtil.validateName(firstName, "First name");
        ValidationUtil.validateName(lastName, "Last name");
    }
}