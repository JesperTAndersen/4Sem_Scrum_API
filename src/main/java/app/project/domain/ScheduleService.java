package app.project.domain;

import app.project.presentation.dto.ScheduleDTO;
import app.task.presentation.dto.StaffingWarning;
import app.task.domain.Task;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScheduleService implements IScheduleService
{
    @Override
    public ScheduleDTO calculateFinishDate(Project project)
    {
        List<StaffingWarning> warnings = project.getStages().stream()
                .flatMap(stage -> stage.getTasks().stream())
                .sorted(Comparator.comparing(Task::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .flatMap(task -> task.getStaffingWarnings().stream())
                .toList();

        List<LocalDate> taskEnds = project.getStages().stream()
                .flatMap(stage -> stage.getTasks().stream())
                .map(task -> WorkingDaySchedule.calculateTaskDates(task, project.getStartDate()).endDate())
                .toList();

        if (taskEnds.stream().anyMatch(Objects::isNull))
        {
            return new ScheduleDTO(null, null, warnings);
        }

        LocalDate calculatedDate = taskEnds.stream()
                .max(LocalDate::compareTo)
                .orElse(project.getStartDate());
        boolean feasible = !calculatedDate.isAfter(project.getDeadline());

        return new ScheduleDTO(calculatedDate, feasible, warnings);
    }
}
