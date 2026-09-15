package app.services.implementations;

import java.util.List;

import app.dtos.task.TaskCreateDTO;
import app.dtos.task.TaskDTO;
import app.entities.Stage;
import app.entities.Task;
import app.persistence.implementations.TaskDAO;
import app.persistence.interfaces.specific.IStageDAO;
import app.services.interfaces.ITaskService;
import app.utils.ValidationUtil;

public class TaskService implements ITaskService
{
    private final TaskDAO taskDAO;
    private final IStageDAO stageDAO;

    public TaskService(TaskDAO taskDAO, IStageDAO stageDAO)
    {
        this.taskDAO = taskDAO;
        this.stageDAO = stageDAO;
    }

    @Override
    public TaskDTO create(TaskCreateDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Task");
        ValidationUtil.validateId(dto.stageId());
        validateName(dto.name());

        Stage stage = stageDAO.get(dto.stageId());
        Task created = taskDAO.create(new Task(stage,
                    dto.name().trim(),
                    dto.estimate(),
                    dto.duration(),
                    dto.crewSize()));
        return toDTO(created);
    }

    @Override
    public TaskDTO get(Long id)
    {
        return toDTO(taskDAO.get(id));
    }

    @Override
    public List<TaskDTO> getAll()
    {
        return taskDAO.getAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public TaskDTO update(TaskDTO dto)
    {
        throw new RuntimeException("TaskService:update not implemented");
    }

    @Override
    public void delete(Long id)
    {
        taskDAO.delete(id);
    }

    private void validateName(String name)
    {
        ValidationUtil.validateNotBlank(name, "Task name");
    }

    private TaskDTO toDTO(Task task)
    {
        return new TaskDTO(task.getId(), task.getName(), task.getEstimate(), task.getDuration(), task.getCrewSize());
    }
}
