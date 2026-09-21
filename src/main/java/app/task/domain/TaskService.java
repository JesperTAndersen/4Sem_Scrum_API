package app.task.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import app.competence.domain.Competence;
import app.competence.presentation.dto.CompetenceDTO;
import app.exceptions.ApiException;
import app.shared.data.IReadDAO;
import app.stage.domain.Stage;
import app.task.data.TaskDAO;
import app.task.data.TaskMapper;
import app.task.presentation.dto.TaskCompetenceDTO;
import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityNotFoundException;

public class TaskService implements ITaskService
{
    private final TaskDAO taskDAO;
    private final IReadDAO<Stage> stageReader;
    private final IReadDAO<Competence> competenceReader;

    public TaskService(TaskDAO taskDAO, IReadDAO<Stage> stageReader, IReadDAO<Competence> competenceReader)
    {
        this.taskDAO = taskDAO;
        this.stageReader = stageReader;
        this.competenceReader = competenceReader;
    }

    @Override
    public TaskDTO create(TaskCreateDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Task");
        ValidationUtil.validateId(dto.stageId());
        validateName(dto.name());

        Stage stage = stageReader.get(dto.stageId());
        Task created = taskDAO.create(new Task(stage,
                    dto.name().trim(),
                    dto.estimate()));
        return TaskMapper.toDTO(created);
    }

    @Override
    public TaskDTO get(Long id)
    {
        return TaskMapper.toDTO(getExistingTask(id));
    }

    @Override
    public List<TaskDTO> getAll()
    {
        return taskDAO.getAll().stream()
                .map(TaskMapper::toDTO)
                .toList();
    }

    @Override
    public TaskDTO update(TaskDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Task");
        return update(dto.id(), new TaskUpdateDTO(dto.name(), dto.estimate()));
    }

    @Override
    public TaskDTO update(Long id, TaskUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        ValidationUtil.validateNotNull(dto, "Task");
        validateName(dto.name());
        validateEstimate(dto.estimate());

        Task task = getExistingTask(id);
        task.update(dto.name().trim(), dto.estimate());
        taskDAO.update(task);
        return get(id);
    }

    @Override
    public void delete(Long id)
    {
        getExistingTask(id);
        taskDAO.delete(id);
    }

    public List<TaskCompetenceDTO> getAllCompetence(Long id)
    {
        return taskDAO.getAllCompetence(id).stream()
                .map(TaskMapper::toDTO)
                .toList();
    }

    public void competence(Long id, TaskCompetenceDTO dto)
    {
        Task task = getExistingTask(id);
        Competence competence = competenceReader.get(dto.competenceId());
        task.assignCompetence(competence, dto.estimate());
        taskDAO.update(task);
    }

    private void validateName(String name)
    {
        ValidationUtil.validateNotBlank(name, "Task name");
    }

    private void validateEstimate(Double estimate)
    {
        if (estimate == null)
        {
            throw new ApiException(400, "Task estimate is required");
        }
    }

    private Set<Competence> getRequiredCompetences(Set<Long> competenceIds)
    {
        if (competenceIds == null || competenceIds.isEmpty())
        {
            throw new ApiException(400, "At least one competence is required");
        }

        return competenceIds.stream()
                .map(this::getExistingCompetence)
                .collect(Collectors.toSet());
    }

    private Competence getExistingCompetence(Long id)
    {
        try
        {
            return competenceReader.get(id);
        }
        catch (EntityNotFoundException e)
        {
            throw new ApiException(404, "Competence not found with id: " + id);
        }
    }

    private Task getExistingTask(Long id)
    {
        try
        {
            return taskDAO.get(id);
        }
        catch (EntityNotFoundException e)
        {
            throw new ApiException(404, "Task not found with id: " + id);
        }
    }

}
