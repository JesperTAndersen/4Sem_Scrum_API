package app.employee.domain;

import app.capacity.data.ICompanyCapacityDAO;
import app.competence.domain.Competence;
import app.employee.data.EmployeeMapper;
import app.employee.data.IEmployeeDAO;
import app.employee.presentation.dto.EmployeeCapacityDTO;
import app.employee.presentation.dto.EmployeeCompetencesUpdateDTO;
import app.employee.presentation.dto.EmployeeCreateDTO;
import app.employee.presentation.dto.EmployeeDTO;
import app.employee.presentation.dto.EmployeeUpdateDTO;
import app.exceptions.BadRequestException;
import app.shared.data.IReadDAO;
import app.utils.ValidationUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EmployeeService implements IEmployeeService
{
    private final IEmployeeDAO employeeDAO;
    private final ICompanyCapacityDAO companyCapacityDAO;
    private final IReadDAO<Competence> competenceReader;

    public EmployeeService(IEmployeeDAO employeeDAO, ICompanyCapacityDAO companyCapacityDAO,
                            IReadDAO<Competence> competenceReader)
    {
        this.employeeDAO = employeeDAO;
        this.companyCapacityDAO = companyCapacityDAO;
        this.competenceReader = competenceReader;
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
    public EmployeeDTO updateCompetences(Long id, EmployeeCompetencesUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        if (dto == null || dto.competenceIds() == null)
        {
            throw new BadRequestException("Competence ids are required");
        }

        Employee employee = employeeDAO.get(id);
        List<Competence> competences = new ArrayList<>();
        for (Long competenceId : new HashSet<>(dto.competenceIds()))
        {
            if (competenceId == null || competenceId <= 0)
            {
                throw new BadRequestException("Competence ids must be positive numbers");
            }
            Competence competence = competenceReader.get(competenceId);
            if (!competence.isActive() && !employee.getCompetences().contains(competence))
            {
                throw new BadRequestException(
                        "Inactive competence cannot be associated with an employee: " + competenceId);
            }
            competences.add(competence);
        }

        employee.replaceCompetences(competences);
        return EmployeeMapper.toDTO(employeeDAO.update(employee));
    }

    @Override
    public EmployeeCapacityDTO calculateDailyCapacity(Long competenceId)
    {
        if (competenceId != null)
        {
            ValidationUtil.validateId(competenceId);
            competenceReader.get(competenceId);
        }

        List<Employee> employees = employeeDAO.getAll().stream()
                .filter(Employee::isActive)
                .filter(employee -> competenceId == null || employee.getCompetences().stream()
                        .anyMatch(competence -> competenceId.equals(competence.getId())))
                .toList();
        double dailyCapacity = employees.stream().mapToDouble(Employee::getDailyCapacity).sum();
        return new EmployeeCapacityDTO(competenceId, employees.size(), dailyCapacity);
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
