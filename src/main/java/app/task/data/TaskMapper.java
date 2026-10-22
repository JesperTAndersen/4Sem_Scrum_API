package app.task.data;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import app.competence.domain.Competence;
import app.competence.presentation.dto.SlimCompetenceDTO;
import app.employee.data.EmployeeMapper;
import app.employee.domain.Employee;
import app.project.domain.WorkingDaySchedule;
import app.task.domain.Task;
import app.employee.presentation.dto.EmployeeReferenceDTO;
import app.task.presentation.dto.TaskDTO;

public final class TaskMapper
{
    private TaskMapper() {}

    public static TaskDTO toDTO(Task task)
    {
        return toDTO(task, projectStartDate(task));
    }

    public static TaskDTO toDTO(Task task, LocalDate projectStartDate)
    {
        Competence competence = task.getCompetence();
        SlimCompetenceDTO competenceDTO = competence == null
                ? null
                : new SlimCompetenceDTO(competence.getId(), competence.getName());
        WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(task, projectStartDate);
        return new TaskDTO(
                task.getId(),
                task.getName(),
                task.getMinimumDurationInDays(),
                competenceDTO,
                task.calcEstimate(),
                task.getBestEstimate(),
                task.getLikelyEstimate(),
                task.getWorstEstimate(),
                task.getCost(),
                dates.startDate(),
                dates.endDate(),
                task.getLaborDurationInDays(),
                task.getScheduledDurationInDays(),
                task.getStatus(),
                task.getStaffingStatus(),
                task.getPredecessors().stream().map(Task::getId).sorted().toList(),
                task.getDependencyStartOffsetInDays(),
                task.getDependencyFinishOffsetInDays(),
                mapToEmployeeReferenceDTOs(task.getEmployees()),
                task.getStaffingWarnings()
        );
    }

    private static LocalDate projectStartDate(Task task)
    {
        return task.getStage() == null || task.getStage().getProject() == null
                ? LocalDate.ofEpochDay(0)
                : task.getStage().getProject().getStartDate();
    }

    private static List<EmployeeReferenceDTO> mapToEmployeeReferenceDTOs(Set<Employee> employees)
    {
        if(employees != null && !employees.isEmpty()) {
            return employees.stream()
                    .sorted(Comparator.comparing(Employee::getId))
                    .map(EmployeeMapper::toEmployeeReferenceDTO)
                    .toList();
        }
        return List.of();
    }

}