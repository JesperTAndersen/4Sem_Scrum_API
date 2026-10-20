package app.project.domain;

import app.project.presentation.dto.ScheduleDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ScheduleService implements IScheduleService
{
    @Override
    public ScheduleDTO calculateFinishDate(Project project)
    {
        List<LocalDate> taskEnds = project.getStages().stream()
                .flatMap(stage -> stage.getTasks().stream())
                .map(task -> WorkingDaySchedule.calculateTaskDates(task, project.getStartDate()).endDate())
                .toList();

        if (taskEnds.stream().anyMatch(Objects::isNull))
        {
            return new ScheduleDTO(null, null);
        }

        LocalDate calculatedDate = taskEnds.stream()
                .max(LocalDate::compareTo)
                .orElse(project.getStartDate());
        boolean feasible = !calculatedDate.isAfter(project.getDeadline());

        return new ScheduleDTO(calculatedDate, feasible);
    }
}
