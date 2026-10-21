package app.task.presentation.dto;

public record StaffingWarning(WarningType warningType, Long taskId, Long employeeId)
{
    public enum WarningType
    {
        INACTIVE_ASSIGNED_EMPLOYEE,
        ASSIGNED_EMPLOYEE_MISSING_COMPETENCE
    }
}
