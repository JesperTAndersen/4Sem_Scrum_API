package app.persistence.testdoubles;

import app.employee.data.IEmployeeDAO;
import app.employee.domain.Employee;
import app.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryEmployeeDAO implements IEmployeeDAO
{
    private final Map<Long, Employee> employees = new LinkedHashMap<>();
    private long nextId = 1L;

    @Override
    public Employee create(Employee employee)
    {
        EntityIds.assign(employee, nextId++);
        employees.put(employee.getId(), employee);
        return employee;
    }

    @Override
    public Employee get(Long id)
    {
        Employee employee = employees.get(id);
        if (employee == null)
        {
            throw new NotFoundException("No employee found with id: " + id);
        }
        return employee;
    }

    @Override
    public List<Employee> getAll()
    {
        return new ArrayList<>(employees.values());
    }

    @Override
    public Employee update(Employee employee)
    {
        get(employee.getId());
        employees.put(employee.getId(), employee);
        return employee;
    }

    @Override
    public boolean delete(Long id)
    {
        get(id);
        return employees.remove(id) != null;
    }

    @Override
    public void setActive(Long id, boolean active)
    {
        // The real DAO's bulk update leaves an unknown ID unchanged.
        Employee employee = employees.get(id);
        if (employee != null)
        {
            employee.setActive(active);
        }
    }
}
