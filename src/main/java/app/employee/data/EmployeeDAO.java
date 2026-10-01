package app.employee.data;

import app.employee.domain.Employee;

import java.util.List;

public class EmployeeDAO implements IEmployeeDAO
{
    @Override
    public Employee create(Employee employee)
    {
        return null;
    }

    @Override
    public boolean delete(Long id)
    {
        return false;
    }

    @Override
    public Employee get(Long id)
    {
        return null;
    }

    @Override
    public List<Employee> getAll()
    {
        return List.of();
    }

    @Override
    public Employee update(Employee employee)
    {
        return null;
    }

    @Override
    public void setActive(Long id, boolean active)
    {

    }
}
