package app.capacity.data;

import app.capacity.domain.CompanyCapacity;
import app.capacity.presentation.dto.CompanyCapacityDTO;

public final class CompanyCapacityMapper
{
    private CompanyCapacityMapper()
    {
    }

    public static CompanyCapacityDTO toDTO(CompanyCapacity companyCapacity)
    {
        if (companyCapacity == null)
        {
            return null;
        }

        return new CompanyCapacityDTO(companyCapacity.getDailyCapacity());
    }
}