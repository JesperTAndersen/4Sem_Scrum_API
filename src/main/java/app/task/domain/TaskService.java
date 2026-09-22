package app.task.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import app.competence.domain.Competence;
import app.competence.presentation.dto.CompetenceDTO;
import app.exceptions.BadRequestException;
import app.exceptions.NotFoundException;
import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import app.stage.domain.Stage;
import app.task.domain.Task;
import app.task.data.TaskDAO;
import app.task.data.TaskMapper;
import app.task.domain.ITaskService;
import app.exceptions.ApiException;
import app.shared.data.IReadDAO;
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
        Set<Competence> competences = getRequiredCompetences(dto.competenceIds(), Set.of());

        Stage stage = stageReader.get(dto.stageId());
        Task created = taskDAO.create(new Task(stage,
                    dto.name().trim(),
                    dto.estimate(),
                    competences));
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
        return update(dto.id(), new TaskUpdateDTO(dto.name(), dto.estimate(),
                dto.competences().stream().map(CompetenceDTO::id).collect(Collectors.toSet())));
    }

    @Override
    public TaskDTO update(Long id, TaskUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        ValidationUtil.validateNotNull(dto, "Task");
        validateName(dto.name());
        validateEstimate(dto.estimate());
        Task task = getExistingTask(id);
        Set<Long> existingCompetenceIds = task.getRequiredCompetences().stream()
                .map(Competence::getId)
                .collect(Collectors.toSet());
        Set<Competence> competences = getRequiredCompetences(dto.competenceIds(), existingCompetenceIds);
        task.update(dto.name().trim(), dto.estimate(), competences);
        taskDAO.update(task);
        return get(id);
    }

    @Override
    public void delete(Long id)
    {
        getExistingTask(id);
        taskDAO.delete(id);
    }

    private void validateName(String name)
    {
        ValidationUtil.validateNotBlank(name, "Task name");
    }

    private void validateEstimate(Double estimate)
    {
        if (estimate == null)
        {
            throw new BadRequestException("Task estimate is required");
        }
    }

    private Set<Competence> getRequiredCompetences(Set<Long> competenceIds, Set<Long> allowedInactiveIds)
    {
        if (competenceIds == null || competenceIds.isEmpty())
        {
            throw new BadRequestException("At least one competence is required");
        }

        return competenceIds.stream()
                .map(this::getExistingCompetence)
                .map(competence ->
                {
                    validateCanBeAssigned(competence, allowedInactiveIds);
                    return competence;
                })
                .collect(Collectors.toSet());
    }

    private void validateCanBeAssigned(Competence competence, Set<Long> allowedInactiveIds)
    {
        if (!competence.isActive() && !allowedInactiveIds.contains(competence.getId()))
        {
            throw new BadRequestException("Inactive competence cannot be assigned to a task: " + competence.getId());
        }
    }

    private Competence getExistingCompetence(Long id)
    {
        try
        {
            return competenceReader.get(id);
        }
        catch (EntityNotFoundException e)
        {
            throw new NotFoundException("Competence not found with id: " + id);
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
            throw new NotFoundException("Task not found with id: " + id);
        }
    }

}
