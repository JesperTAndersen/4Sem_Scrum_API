package app.task.domain;

import java.util.List;

import app.competence.domain.Competence;
import app.shared.data.IReadDAO;
import app.stage.domain.Stage;
import app.task.data.TaskDAO;
import app.task.data.TaskMapper;
import app.exceptions.BadRequestException;
import app.exceptions.NotFoundException;
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
        validateEstimate(dto.minDuration());
        validateEstimate(dto.estimate());
        validateMinimumDuration(dto.minimumDurationInDays());
        Set<Competence> competences = getRequiredCompetences(dto.competenceIds(), Set.of());

        Stage stage = stageReader.get(dto.stageId());
        Task created = taskDAO.create(new Task(stage,
                    dto.name().trim(),
                    dto.minDuration()));
                    dto.estimate(),
                    dto.minimumDurationInDays(),
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
        return update(dto.id(), new TaskUpdateDTO(dto.name(), dto.estimate(), dto.minimumDurationInDays(),
                dto.competences().stream().map(CompetenceDTO::id).collect(Collectors.toSet())));
        return update(dto.id(), new TaskUpdateDTO(dto.name(), dto.minDuration(), dto.competenceId(), dto.estimate()));
    }

    @Override
    public TaskDTO update(Long id, TaskUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        ValidationUtil.validateNotNull(dto, "Task");
        validateName(dto.name());
        validateEstimate(dto.estimate());
        validateMinimumDuration(dto.minimumDurationInDays());
        Task task = getExistingTask(id);
        Set<Long> existingCompetenceIds = task.getRequiredCompetences().stream()
                .map(Competence::getId)
                .collect(Collectors.toSet());
        Set<Competence> competences = getRequiredCompetences(dto.competenceIds(), existingCompetenceIds);
        task.update(dto.name().trim(), dto.estimate(), dto.minimumDurationInDays(), competences);
        Task task = getExistingTask(id);
        task.update(dto.name(), dto.minDuration());
        if (dto.competenceId() != null) {
            if (dto.competenceId() == 0) {
                task.setCompetence(null, 0.0f);
            } else {
                validateEstimate(dto.estimate());
                Competence competence = getExistingCompetence(dto.competenceId());
                validateCanBeAssigned(competence);
                task.setCompetence(competence, dto.estimate().floatValue());
            }
        }
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

        if (!Double.isFinite(estimate) || estimate < 0)
        {
            throw new BadRequestException("Task estimate must be a non-negative number of hours");
        }
    }

    private void validateMinimumDuration(Integer minimumDurationInDays)
    {
        if (minimumDurationInDays == null)
        {
            throw new BadRequestException("Task minimum duration is required");
        }

        if (minimumDurationInDays < 0)
        {
            throw new BadRequestException("Task minimum duration must be a non-negative number of days");
        }
    }

    private Set<Competence> getRequiredCompetences(Set<Long> competenceIds, Set<Long> allowedInactiveIds)
    private void validateCanBeAssigned(Competence competence)
    {
        if (!competence.isActive())
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
