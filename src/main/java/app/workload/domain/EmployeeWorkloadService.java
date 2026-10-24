package app.workload.domain;

import app.competence.domain.Competence;
import app.competence.presentation.dto.SlimCompetenceDTO;
import app.employee.domain.Employee;
import app.workload.presentation.dto.AvailabilityResultDTO;
import app.workload.presentation.dto.EmployeeAvailabilityDTO;
import app.exceptions.BadRequestException;
import app.workload.presentation.dto.EmployeeOverAllocationDTO;
import app.workload.presentation.dto.OverAllocationResultDTO;
import app.workload.presentation.dto.TaskHoursDTO;
import app.workload.presentation.dto.UnassessedTaskDTO;
import app.project.domain.Project;
import app.project.domain.WorkingDaySchedule;
import app.shared.data.IReadDAO;
import app.task.domain.Task;
import app.utils.ValidationUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EmployeeWorkloadService implements IEmployeeWorkloadService
{
    private static final double OVER_ALLOCATION_TOLERANCE = 0.000001;
    private static final String UNDETERMINED_SCHEDULE = "UNDETERMINED_SCHEDULE";

    private final IReadDAO<Task> taskReader;
    private final IReadDAO<Employee> employeeReader;

    public EmployeeWorkloadService(IReadDAO<Task> taskReader, IReadDAO<Employee> employeeReader)
    {
        this.taskReader = taskReader;
        this.employeeReader = employeeReader;
    }

    @Override
    public OverAllocationResultDTO findConflicts(LocalDate startDate, LocalDate endDate)
    {
        validateDates(startDate, endDate);

        List<Employee> employees = new ArrayList<>();
        List<UnassessedTaskDTO> unassessedTasks = new ArrayList<>();
        List<TaskWorkload> taskWorkloads = getTaskWorkloads(startDate, endDate, unassessedTasks);

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

        unassessedTasks.sort(Comparator.comparing(UnassessedTaskDTO::taskId));
        return new OverAllocationResultDTO(startDate, endDate, List.copyOf(conflicts),
                getTaskIds(unassessedTasks), List.copyOf(unassessedTasks));
    }

    @Override
    public AvailabilityResultDTO findAvailability(LocalDate date, Long competenceId)
    {
        if (date == null)
        {
            throw new BadRequestException("Availability date is required");
        }

        List<Employee> employees = new ArrayList<>(employeeReader.getAll());

        if (competenceId != null)
        {
            ValidationUtil.validateId(competenceId);
            employees.removeIf(employee -> employee.getCompetences().stream()
                    .noneMatch(competence -> competenceId.equals(competence.getId())));
        }
        employees.sort(Comparator.comparing(Employee::getId));

        List<UnassessedTaskDTO> unassessedTasks = new ArrayList<>();
        List<EmployeeAvailabilityDTO> availability = new ArrayList<>();
        List<TaskWorkload> taskWorkloads = getTaskWorkloads(date, date, unassessedTasks);

        boolean workingDay = WorkingDaySchedule.isWorkingDay(date);
        for (Employee employee : employees)
        {
            if (employee.isActive() && workingDay)
            {
                double allocatedHours = totalHours(getDailyTaskHours(employee, date, taskWorkloads));
                double dailyCapacity = employee.getDailyCapacity();
                double remainingHours = Math.max(0, dailyCapacity - allocatedHours);
                availability.add(new EmployeeAvailabilityDTO(
                        employee.getId(),
                        employee.getFirstName(),
                        employee.getLastName(),
                        dailyCapacity,
                        allocatedHours,
                        remainingHours,
                        getCompetences(employee)));
            }
        }

        unassessedTasks.sort(Comparator.comparing(UnassessedTaskDTO::taskId));
        return new AvailabilityResultDTO(date, List.copyOf(availability),
                getTaskIds(unassessedTasks), List.copyOf(unassessedTasks));
    }

    private List<SlimCompetenceDTO> getCompetences(Employee employee)
    {
        return employee.getCompetences().stream()
                .sorted(Comparator.comparing(Competence::getId))
                .map(competence -> new SlimCompetenceDTO(competence.getId(), competence.getName()))
                .toList();
    }

    private List<TaskWorkload> getTaskWorkloads(LocalDate startDate, LocalDate endDate,
                                               List<UnassessedTaskDTO> unassessedTasks)
    {
        List<TaskWorkload> taskWorkloads = new ArrayList<>();
        for (Task task : taskReader.getAll())
        {
            Project project = task.getStage().getProject();
            calculateTaskWorkload(project, task, startDate, endDate, taskWorkloads, unassessedTasks);
        }
        return taskWorkloads;
    }

    private static List<Long> getTaskIds(List<UnassessedTaskDTO> unassessedTasks)
    {
        return unassessedTasks.stream()
                .map(UnassessedTaskDTO::taskId)
                .toList();
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
                                       List<TaskWorkload> taskWorkloads, List<UnassessedTaskDTO> unassessedTasks)
    {
        if (task.getStatus() == Task.TaskStatus.DONE || task.calcEstimate() == 0)
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
            unassessedTasks.add(new UnassessedTaskDTO(
                    task.getId(),
                    task.getName(),
                    project.getId(),
                    project.getTitle(),
                    getUnassessedReason(task)));
            return;
        }

        List<Employee> availableEmployees = task.getAvailableEmployees();
        double totalDailyCapacity = 0;

        for (Employee employee : availableEmployees)
        {
            totalDailyCapacity += employee.getDailyCapacity();
        }

        long workingDays = WorkingDaySchedule.countWorkingDays(dates.startDate(), dates.endDate());
        double estimatedHours = task.calcEstimate();

        for (Employee employee : availableEmployees)
        {
            double employeeShare = employee.getDailyCapacity() / totalDailyCapacity;
            double employeeTaskHours = estimatedHours * employeeShare;
            double dailyTaskHours = employeeTaskHours / workingDays;

            Competence competence = task.getCompetence();
            TaskHoursDTO taskHours = new TaskHoursDTO(
                    project.getId(),
                    project.getTitle(),
                    task.getId(),
                    task.getName(),
                    competence == null ? null : competence.getId(),
                    competence == null ? null : competence.getName(),
                    dailyTaskHours);

            taskWorkloads.add(new TaskWorkload(employee, dates.startDate(), dates.endDate(), taskHours));
        }
    }

    private static String getUnassessedReason(Task task)
    {
        if (task.getStaffingStatus() != Task.StaffingStatus.STAFFED)
        {
            return task.getStaffingStatus().name();
        }
        return UNDETERMINED_SCHEDULE;
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
