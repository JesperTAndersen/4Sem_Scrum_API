package app.employee.data;

import app.employee.domain.Employee;
import app.shared.data.ICrudDAO;

/**
 * Data-access contract reserved for employee persistence.
 */
public interface IEmployeeDAO extends ICrudDAO<Employee>
{
    void setActive(Long id, boolean active);
}
