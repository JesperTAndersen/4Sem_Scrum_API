package app.workload.domain;

import app.employee.domain.Employee;
import app.exceptions.BadRequestException;
import app.workload.presentation.dto.EmployeeOverAllocationDTO;
import app.workload.presentation.dto.OverAllocationResultDTO;
import app.workload.presentation.dto.TaskHoursDTO;
import app.project.domain.Project;
import app.project.domain.WorkingDaySchedule;
import app.shared.data.IReadDAO;
import app.task.domain.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EmployeeWorkloadService implements IEmployeeWorkloadService
{
    private static final double OVER_ALLOCATION_TOLERANCE = 0.000001;

    private final IReadDAO<Task> taskReader;

    public EmployeeWorkloadService(IReadDAO<Task> taskReader)
    {
        this.taskReader = taskReader;
    }

    @Override
    public OverAllocationResultDTO findConflicts(LocalDate startDate, LocalDate endDate)
    {
        validateDates(startDate, endDate);

        List<Employee> employees = new ArrayList<>();
        List<Long> unassessedTaskIds = new ArrayList<>();
        List<TaskWorkload> taskWorkloads = getTaskWorkloads(startDate, endDate, unassessedTaskIds);

        for (TaskWorkload taskWorkload : taskWorkloads)
        {
            if (!employees.contains(taskWorkload.employee()))
            {
                employees.add(taskWorkload.employee());
            }
        }

        employees.sort(Comparator.comparing(Employee::getId));

        List<EmployeeOverAllocationDTO> conflicts = new ArrayList<>();
        for (Employee employee : employees)
        {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1))
            {
                if (WorkingDaySchedule.isWorkingDay(date))
                {
                    EmployeeOverAllocationDTO conflict = findDailyOverAllocation(employee, date, taskWorkloads);

                    if (conflict != null)
                    {
                        conflicts.add(conflict);
                    }
                }
                if (date.equals(endDate))
                {
                    break;
                }
            }
        }

        unassessedTaskIds.sort(Long::compareTo);
        return new OverAllocationResultDTO(startDate, endDate, List.copyOf(conflicts), List.copyOf(unassessedTaskIds));
    }

    private List<TaskWorkload> getTaskWorkloads(LocalDate startDate, LocalDate endDate,
                                               List<Long> unassessedTaskIds)
    {
        List<TaskWorkload> taskWorkloads = new ArrayList<>();
        for (Task task : taskReader.getAll())
        {
            Project project = task.getStage().getProject();
            calculateTaskWorkload(project, task, startDate, endDate, taskWorkloads, unassessedTaskIds);
        }
        return taskWorkloads;
    }

    private static void validateDates(LocalDate startDate, LocalDate endDate)
    {
        if (startDate == null || endDate == null)
        {
            throw new BadRequestException("Planning start and end dates are required");
        }
        if (endDate.isBefore(startDate))
        {
            throw new BadRequestException("Planning end date cannot be before start date");
        }
    }

    private void calculateTaskWorkload(Project project, Task task, LocalDate startDate, LocalDate endDate,
                                       List<TaskWorkload> taskWorkloads, List<Long> unassessedTaskIds)
    {
        if (task.getStatus() == Task.TaskStatus.DONE || task.getEstimate() == 0)
        {
            return;
        }

        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(task, project.getStartDate());
        if (dates.isDetermined() && (dates.endDate().isBefore(startDate) || dates.startDate().isAfter(endDate)))
        {
            return;
        }

        if (!dates.isDetermined() || task.getStaffingStatus() != Task.StaffingStatus.STAFFED)
        {
            unassessedTaskIds.add(task.getId());
            return;
        }

        List<Employee> availableEmployees = task.getAvailableEmployees();
        double totalDailyCapacity = 0;

        for (Employee employee : availableEmployees)
        {
            totalDailyCapacity += employee.getDailyCapacity();
        }

        long workingDays = WorkingDaySchedule.countWorkingDays(dates.startDate(), dates.endDate());
        double estimatedHours = task.getEstimate();

        for (Employee employee : availableEmployees)
        {
            double employeeShare = employee.getDailyCapacity() / totalDailyCapacity;
            double employeeTaskHours = estimatedHours * employeeShare;
            double dailyTaskHours = employeeTaskHours / workingDays;

            TaskHoursDTO taskHours = new TaskHoursDTO(
                    project.getId(),
                    project.getTitle(),
                    task.getId(),
                    task.getName(),
                    dailyTaskHours);

            taskWorkloads.add(new TaskWorkload(employee, dates.startDate(), dates.endDate(), taskHours));
        }
    }

    private EmployeeOverAllocationDTO findDailyOverAllocation(Employee employee, LocalDate date,
                                                              List<TaskWorkload> taskWorkloads)
    {
        List<TaskHoursDTO> taskHours = getDailyTaskHours(employee, date, taskWorkloads);
        double dailyHours = totalHours(taskHours);

        double availableHours = employee.getDailyCapacity();
        double excessHours = dailyHours - availableHours;
        if (dailyHours <= availableHours || dailyHours < availableHours + OVER_ALLOCATION_TOLERANCE)
        {
            return null;
        }

        return new EmployeeOverAllocationDTO(employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                date,
                availableHours,
                dailyHours,
                excessHours,
                List.copyOf(taskHours));
    }

    private List<TaskHoursDTO> getDailyTaskHours(Employee employee, LocalDate date,
                                                 List<TaskWorkload> taskWorkloads)
    {
        List<TaskHoursDTO> taskHours = new ArrayList<>();

        for (TaskWorkload taskWorkload : taskWorkloads)
        {
            if (!employee.equals(taskWorkload.employee()))
            {
                continue;
            }
            if (date.isBefore(taskWorkload.startDate()) || date.isAfter(taskWorkload.endDate()))
            {
                continue;
            }
            taskHours.add(taskWorkload.taskHours());
        }

        taskHours.sort(Comparator.comparing(TaskHoursDTO::taskId));
        return taskHours;
    }

    private double totalHours(List<TaskHoursDTO> taskHours)
    {
        double hours = 0;
        for (TaskHoursDTO task : taskHours)
        {
            hours += task.allocatedHours();
        }
        return hours;
    }

    private record TaskWorkload(Employee employee,
                                LocalDate startDate,
                                LocalDate endDate,
                                TaskHoursDTO taskHours) {}
}
