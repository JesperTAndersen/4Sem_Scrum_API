package app.project.domain;

import app.project.presentation.dto.ScheduleDTO;

import java.time.LocalDate;

public class ScheduleService implements IScheduleService
{
    @Override
    public ScheduleDTO calculateFinishDate(Project project)
    {
        LocalDate calculatedDate = project.getStages().stream()
                .flatMap(stage -> stage.getTasks().stream())
                .map(task -> WorkingDaySchedule.calculateTaskDates(task, project.getStartDate()).endDate())
                .max(LocalDate::compareTo)
                .orElse(project.getStartDate());
        boolean feasible = !calculatedDate.isAfter(project.getDeadline());

        return new ScheduleDTO(calculatedDate, feasible);
    }
}
