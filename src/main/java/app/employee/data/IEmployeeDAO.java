package app.employee.data;

import app.employee.domain.Employee;
import app.shared.data.ICrudDAO;

/**
 * Data-access contract reserved for employee persistence.
 */
public interface IEmployeeDAO extends ICrudDAO
{
    void setActive(Long id, boolean active);
}
