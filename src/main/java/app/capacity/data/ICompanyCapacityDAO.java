package app.capacity.data;

import app.capacity.domain.CompanyCapacity;
import app.shared.data.IUpdateDAO;

public interface ICompanyCapacityDAO extends IUpdateDAO<CompanyCapacity>
{
    void initialize();

    CompanyCapacity get();
}