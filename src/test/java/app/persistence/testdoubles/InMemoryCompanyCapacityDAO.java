package app.persistence.testdoubles;

import app.capacity.data.ICompanyCapacityDAO;
import app.capacity.domain.CompanyCapacity;

public class InMemoryCompanyCapacityDAO implements ICompanyCapacityDAO
{
    private CompanyCapacity companyCapacity = new CompanyCapacity();

    @Override
    public void initialize()
    {
        // A fresh test DAO already has the singleton setting initialized.
    }

    @Override
    public CompanyCapacity get()
    {
        return companyCapacity;
    }

    @Override
    public CompanyCapacity update(CompanyCapacity companyCapacity)
    {
        this.companyCapacity = companyCapacity;
        return companyCapacity;
    }
}
