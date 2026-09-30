package app.project.domain;

import app.task.domain.Task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class WorkingDaySchedule
{
    private WorkingDaySchedule()
    {
    }

    public record TaskDates(LocalDate startDate, LocalDate endDate)
    {
    }

    public static TaskDates calculateTaskDates(Task task, LocalDate projectStartDate)
    {
        if (task == null)
        {
            throw new IllegalArgumentException("Task is required");
        }
        if (projectStartDate == null)
        {
            throw new IllegalArgumentException("Project start date is required");
        }

        return calculate(task, nextWorkingDay(projectStartDate), new HashMap<>());
    }

    private static TaskDates calculate(Task task, LocalDate projectStartDate, Map<Task, TaskDates> calculated)
    {
        TaskDates existing = calculated.get(task);

        if (existing != null)
        {
            return existing;
        }

        LocalDate startDate = projectStartDate;
        for (Task predecessor : task.getPredecessors())
        {
            TaskDates predecessorDates = calculate(predecessor, projectStartDate, calculated);
            LocalDate availableDate = predecessor.getScheduledDurationInDays() == 0
                    ? predecessorDates.startDate()
                    : nextWorkingDay(predecessorDates.endDate().plusDays(1));
            if (availableDate.isAfter(startDate)) startDate = availableDate;
        }

        long workingDays = requiredWorkingDays(task.getScheduledDurationInDays());
        LocalDate endDate = workingDays == 0 ? startDate : addWorkingDays(startDate, workingDays - 1);
        TaskDates result = new TaskDates(startDate, endDate);
        calculated.put(task, result);
        return result;
    }

    private static long requiredWorkingDays(double durationInDays)
    {
        return durationInDays == 0 ? 0 : (long) Math.ceil(durationInDays);
    }

    private static LocalDate addWorkingDays(LocalDate date, long daysToAdd)
    {
        LocalDate result = date;
        for (long day = 0; day < daysToAdd; day++)
        {
            result = nextWorkingDay(result.plusDays(1));
        }
        return result;
    }

    private static LocalDate nextWorkingDay(LocalDate date)
    {
        LocalDate result = date;
        while (result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY)
        {
            result = result.plusDays(1);
        }
        return result;
    }
}
