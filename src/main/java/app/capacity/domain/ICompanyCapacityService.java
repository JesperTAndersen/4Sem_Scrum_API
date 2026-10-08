package app.capacity.domain;

import app.capacity.presentation.dto.CompanyCapacityDTO;
import app.capacity.presentation.dto.UpdateCompanyCapacityDTO;

public interface ICompanyCapacityService
{
    CompanyCapacityDTO get();

    CompanyCapacityDTO update(UpdateCompanyCapacityDTO dto);
}