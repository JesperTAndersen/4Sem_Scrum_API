package app.workload.presentation.dto;

public record EmployeeAvailabilityDTO(
        Long employeeId,
        String firstName,
        String lastName,
        double dailyCapacity,
        double allocatedHours,
        double remainingHours)
{}