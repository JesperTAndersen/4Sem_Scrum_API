package app.task.domain;

import app.competence.domain.Competence;
import app.exceptions.BadRequestException;
import app.exceptions.NotFoundException;
import app.shared.data.IReadDAO;
import app.stage.domain.Stage;
import app.task.data.TaskDAO;
import app.task.data.TaskMapper;
import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;

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

    @Override public TaskDTO create(TaskCreateDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Task");
        ValidationUtil.validateId(dto.stageId());
        ValidationUtil.validateId(dto.competenceId());
        validateName(dto.name());
        validateEstimate(dto.estimate());
        validateMinimumDuration(dto.minimumDurationInDays());
        Stage stage = stageReader.get(dto.stageId());
        Competence competence = getExistingCompetence(dto.competenceId());
        validateCanBeAssigned(competence);
        Task task = new Task(stage, dto.name().trim(), dto.minimumDurationInDays());
        task.setCompetence(competence, dto.estimate());
        return TaskMapper.toDTO(taskDAO.create(task));
    }

    @Override public TaskDTO get(Long id) { return TaskMapper.toDTO(getExistingTask(id)); }

    @Override public List<TaskDTO> getAll()
    {
        return taskDAO.getAll().stream().map(TaskMapper::toDTO).toList();
    }

    @Override public TaskDTO update(TaskDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Task");
        return update(dto.id(),
                new TaskUpdateDTO(dto.name(),
                        dto.minimumDurationInDays(),
                        dto.competenceId(),
                        dto.estimate(),
                        dto.status()
                )
        );
    }

    @Override public TaskDTO update(Long id, TaskUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        ValidationUtil.validateNotNull(dto, "Task");
        if (dto.name() != null) validateName(dto.name());
        if (dto.minimumDurationInDays() != null) validateMinimumDuration(dto.minimumDurationInDays());

        Task task = getExistingTask(id);
        task.update(dto.name(), dto.minimumDurationInDays());
        if (dto.status() != null)
        {
            task.changeStatus(dto.status());
        }
        if (dto.competenceId() != null)
        {
            if (dto.competenceId() == 0)
            {
                task.setCompetence(null, 0.0);
            }
            else
            {
                validateEstimate(dto.estimate());
                Competence competence = getExistingCompetence(dto.competenceId());
                validateCanBeAssigned(competence);
                task.setCompetence(competence, dto.estimate());
            }
        }
        else if (dto.estimate() != null)
        {
            throw new BadRequestException("A competence id is required when updating a task estimate");
        }
        taskDAO.update(task);
        return get(id);
    }

    @Override public void delete(Long id) { getExistingTask(id); taskDAO.delete(id); }

    // TODO added in feat/task-dependencies
    @Override
    public void addPredecessor(Long taskId, Long predecessorId) {
        ValidationUtil.validateId(taskId);
        ValidationUtil.validateId(predecessorId);

        Task task = getExistingTask(taskId);
        Task predecessor = getExistingTask(predecessorId);

        if (!task.getStage().getProject().getId().equals(predecessor.getStage().getProject().getId())) {
            throw new BadRequestException("Tasks must be in the same project");
        }

        if (task.equals(predecessor)) {
            throw new BadRequestException("Task cannot depend on itself");
        }

        if (task.wouldCreateCycleWith(predecessor)) {
            throw new BadRequestException("Circular dependency detected");
        }

        if (task.getPredecessors().contains(predecessor)) {
            throw new BadRequestException("Dependency already exists");
        }

        task.addPredecessor(predecessor);
        taskDAO.update(task);
    }

    private void validateName(String name) { ValidationUtil.validateNotBlank(name, "Task name"); }

    private void validateMinimumDuration(Integer duration)
    {
        if (duration == null || duration < 0) throw new BadRequestException("Task minimum duration must be a non-negative number of days");
    }

    private void validateEstimate(Double estimate)
    {
        if (estimate == null || !Double.isFinite(estimate) || estimate < 0) throw new BadRequestException("Task estimate must be a non-negative number of hours");
    }

    private void validateCanBeAssigned(Competence competence)
    {
        if (!competence.isActive())
        {
            throw new BadRequestException("Inactive competence cannot be assigned to a task: " + competence.getId());
        }
    }

    private Competence getExistingCompetence(Long id)
    {
        try { return competenceReader.get(id); }
        catch (EntityNotFoundException e) { throw new NotFoundException("Competence not found with id: " + id); }
    }

    private Task getExistingTask(Long id)
    {
        try { return taskDAO.get(id); }
        catch (EntityNotFoundException e) { throw new NotFoundException("Task not found with id: " + id); }
    }
}
