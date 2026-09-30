package app.project.domain;

import app.project.presentation.dto.ScheduleDTO;

public interface IScheduleService
{
    ScheduleDTO calculateFinishDate(Project project);
}
