package app.project.domain;

import app.project.presentation.dto.ScheduleDTO;

import java.time.LocalDate;

public class ScheduleService implements IScheduleService
{
    @Override
    public ScheduleDTO calculateFinishDate(Project project)
    {
        //TODO implement logic etc stream calculated date data from related stages or tasks.
        LocalDate calculatedDate = LocalDate.now();
        boolean feasible = !calculatedDate.isAfter(project.getDeadline());

        return new ScheduleDTO(calculatedDate, feasible);
    }
}
