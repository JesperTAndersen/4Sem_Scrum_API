package app.project.domain;

import app.stage.domain.Stage;
import app.task.domain.Task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorkingDaySchedule
{
    private WorkingDaySchedule()
    {
    }

    public record TaskDates(LocalDate startDate, LocalDate endDate)
    {
        public static final TaskDates UNDETERMINED = new TaskDates(null, null);

        public boolean isDetermined()
        {
            return startDate != null && endDate != null;
        }
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

        return calculate(task, nextWorkingDay(projectStartDate), new HashMap<>(), new HashSet<>());
    }

    private static TaskDates calculate(Task task, LocalDate projectStartDate, Map<Task, TaskDates> calculated, Set<Task> visiting)
    {
        TaskDates existing = calculated.get(task);

        if (existing != null)
        {
            return existing;
        }
        if (task.getStatus() == Task.TaskStatus.DONE && task.hasPlanningSnapshot())
        {
            TaskDates dates = new TaskDates(
                    task.getCompletedStartDate(),
                    task.getCompletedEndDate()
            );
            return remember(task, dates, calculated, visiting);
        }

        if (!visiting.add(task))
        {
            throw new IllegalStateException("Cyclic schedule dependency");
        }
        LocalDate startDate = projectStartDate;
        for (Task predecessor : getPredecessorTasks(task))
        {
            TaskDates predecessorDates = calculate(predecessor, projectStartDate, calculated, visiting);
            if (!predecessorDates.isDetermined())
            {
                return remember(task, TaskDates.UNDETERMINED, calculated, visiting);
            }
            LocalDate availableDate = predecessor.getScheduledDurationInDays() == 0
                    ? predecessorDates.startDate()
                    : nextWorkingDay(predecessorDates.endDate().plusDays(1));
            if (availableDate.isAfter(startDate))
            {
                startDate = availableDate;
            }
        }

        Double duration = task.getScheduledDurationInDays();
        if (duration == null)
        {
            return remember(task, TaskDates.UNDETERMINED, calculated, visiting);
        }
        long workingDays = requiredWorkingDays(duration);
        LocalDate endDate = workingDays == 0 ? startDate : addWorkingDays(startDate, workingDays - 1);
        return remember(task, new TaskDates(startDate, endDate), calculated, visiting);
    }

    private static List<Task> getPredecessorTasks(Task task)
    {
        List<Task> predecessors = new ArrayList<>(task.getPredecessors());
        if (task.getStage() != null)
        {
            for (Stage stage : task.getStage().getPredecessors())
            {
                predecessors.addAll(stage.getTasks());
            }
        }
        return predecessors;
    }

    private static TaskDates remember(Task task, TaskDates dates, Map<Task, TaskDates> calculated, Set<Task> visiting)
    {
        visiting.remove(task);
        calculated.put(task, dates);
        return dates;
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
        while (!isWorkingDay(result))
        {
            result = result.plusDays(1);
        }
        return result;
    }

    public static boolean isWorkingDay(LocalDate date)
    {
        return !date.getDayOfWeek().equals(DayOfWeek.SATURDAY) && !date.getDayOfWeek().equals(DayOfWeek.SUNDAY);
    }


    public static long countWorkingDays(LocalDate from, LocalDate to)
    {
        long count = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1))
        {
            if (isWorkingDay(date))
            {
                count++;
            }
            if (date.equals(to))
            {
                break;
            }
        }
        return count;
    }
}