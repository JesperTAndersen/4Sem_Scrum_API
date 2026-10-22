package app.persistence.testdoubles;

import app.exceptions.NotFoundException;
import app.shared.data.IReadDAO;
import app.task.domain.Task;

import java.util.ArrayList;
import java.util.List;

public class InMemoryTaskReader implements IReadDAO<Task>
{
    private final List<Task> tasks = new ArrayList<>();

    @Override
    public Task get(Long id)
    {
        for (Task task : tasks)
        {
            if (id.equals(task.getId()))
            {
                return task;
            }
        }
        throw new NotFoundException("No task found with id: " + id);
    }

    @Override
    public List<Task> getAll()
    {
        return List.copyOf(tasks);
    }

    public void seed(Task task)
    {
        tasks.add(task);
    }
}
