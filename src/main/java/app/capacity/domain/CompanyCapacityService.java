package app.capacity.domain;

import app.capacity.data.CompanyCapacityMapper;
import app.capacity.data.ICompanyCapacityDAO;
import app.capacity.presentation.dto.CompanyCapacityDTO;
import app.capacity.presentation.dto.UpdateCompanyCapacityDTO;
import app.exceptions.BadRequestException;

public class CompanyCapacityService implements ICompanyCapacityService
{
    private final ICompanyCapacityDAO companyCapacityDAO;

    public CompanyCapacityService(ICompanyCapacityDAO companyCapacityDAO)
    {
        this.companyCapacityDAO = companyCapacityDAO;
    }

    @Override
    public CompanyCapacityDTO get()
    {
        return CompanyCapacityMapper.toDTO(companyCapacityDAO.get());
    }

    @Override
    public CompanyCapacityDTO update(UpdateCompanyCapacityDTO dto)
    {
        validateUpdate(dto);
        CompanyCapacity companyCapacity = companyCapacityDAO.get();
        companyCapacity.update(dto.dailyCapacity());
        return CompanyCapacityMapper.toDTO(companyCapacityDAO.update(companyCapacity));
    }

    private void validateUpdate(UpdateCompanyCapacityDTO dto)
    {
        if (dto == null || dto.dailyCapacity() == null
                || !Double.isFinite(dto.dailyCapacity()) || dto.dailyCapacity() <= 0)
        {
            throw new BadRequestException("Daily capacity must be a finite positive number");
        }
    }
}