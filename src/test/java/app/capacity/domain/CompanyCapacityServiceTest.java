package app.capacity.domain;

import app.capacity.presentation.dto.CompanyCapacityDTO;
import app.capacity.presentation.dto.UpdateCompanyCapacityDTO;
import app.exceptions.ApiException;
import app.persistence.testdoubles.InMemoryCompanyCapacityDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompanyCapacityServiceTest
{
    private CompanyCapacityService companyCapacityService;

    @BeforeEach
    void setUp()
    {
        companyCapacityService = new CompanyCapacityService(new InMemoryCompanyCapacityDAO());
    }

    @Test
    @DisplayName("Get - returns the default standard daily capacity of 7.5 hours")
    void getReturnsDefaultCapacity()
    {
        assertThat(companyCapacityService.get().dailyCapacity(), is(7.5));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(resources = "/testcases/capacity/company-capacity-service/update-fields.csv",
            numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - saves a finite positive capacity and leaves the setting unchanged for invalid input")
    void updateValidatesCapacity(boolean payloadPresent, Double dailyCapacity, String expected, String reason)
    {
        UpdateCompanyCapacityDTO dto = payloadPresent ? new UpdateCompanyCapacityDTO(dailyCapacity) : null;
        CompanyCapacityDTO before = companyCapacityService.get();
        if (expected.equals("ACCEPT"))
        {
            CompanyCapacityDTO result = companyCapacityService.update(dto);
            assertThat(result.dailyCapacity(), is(dailyCapacity));
            assertThat(companyCapacityService.get(), is(result));
        }
        else
        {
            ApiException exception = assertThrows(ApiException.class, () -> companyCapacityService.update(dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
            assertThat(companyCapacityService.get(), is(before));
        }
    }
}
