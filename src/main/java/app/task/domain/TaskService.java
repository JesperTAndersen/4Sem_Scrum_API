package app.task.domain;

import java.util.List;

import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.exceptions.BadRequestException;
import app.exceptions.NotFoundException;
import app.project.domain.WorkingDaySchedule;
import app.shared.data.IReadDAO;
import app.stage.domain.Stage;
import app.task.data.ITaskDAO;
import app.task.data.TaskMapper;
import app.task.presentation.dto.TaskCreateDTO;
import app.task.presentation.dto.TaskDTO;
import app.task.presentation.dto.TaskEmployeesDTO;
import app.task.presentation.dto.TaskUpdateDTO;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityNotFoundException;

public class TaskService implements ITaskService
{
    private final ITaskDAO taskDAO;
    private final IReadDAO<Stage> stageReader;
    private final IReadDAO<Competence> competenceReader;
    private final IReadDAO<Employee> employeeReader;

    public TaskService(ITaskDAO taskDAO, IReadDAO<Stage> stageReader, IReadDAO<Competence> competenceReader, IReadDAO<Employee> employeeReader)
    {
        this.taskDAO = taskDAO;
        this.stageReader = stageReader;
        this.competenceReader = competenceReader;
        this.employeeReader = employeeReader;
    }

    @Override
    public TaskDTO create(TaskCreateDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Task");
        ValidationUtil.validateId(dto.stageId());
        ValidationUtil.validateId(dto.competenceId());
        validateName(dto.name());
        validateEstimate(dto.bestEstimate());
        validateEstimate(dto.likelyEstimate());
        validateEstimate(dto.worstEstimate());
        validateMinimumDuration(dto.minimumDurationInDays());
        Stage stage = stageReader.get(dto.stageId());
        Competence competence = getExistingCompetence(dto.competenceId());
        validateCanBeAssigned(competence);
        if (stage.isCompleted() && stageReader.getAll().stream().anyMatch(s ->
                s.getPredecessors().contains(stage) && s.getTasks().stream().anyMatch(t ->
                        t.getStatus() != Task.TaskStatus.NOT_STARTED)))
        {
            throw new BadRequestException("Cannot add unfinished task to completed stage with started dependents");
        }
        Task task = new Task(stage, dto.name().trim(), dto.minimumDurationInDays());
        task.setCompetence(competence, dto.bestEstimate(), dto.likelyEstimate(), dto.worstEstimate());
        return TaskMapper.toDTO(taskDAO.create(task));
    }

    @Override
    public TaskDTO get(Long id)
    {
        return TaskMapper.toDTO(getExistingTask(id));
    }

    @Override
    public List<TaskDTO> getAll()
    {
        return taskDAO.getAll().stream().map(TaskMapper::toDTO).toList();
    }

    @Override
    public TaskDTO update(TaskDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Task");
        return update(dto.id(),
                new TaskUpdateDTO(dto.name(),
                        dto.minimumDurationInDays(),
                        dto.competence() == null ? 0L : dto.competence().id(),
                        dto.bestEstimate(),
                        dto.likelyEstimate(),
                        dto.worstEstimate(),
                        dto.status()
                )
        );
    }

    @Override
    public TaskDTO update(Long id, TaskUpdateDTO dto)
    {
        validateUpdateRequest(id, dto);
        Task task = getExistingTask(id);
        validateStatusChange(task, dto.status());

        task.update(dto.name(), dto.minimumDurationInDays());
        updateCompetence(task, dto);
        updateStatus(task, dto.status());

        taskDAO.update(task);
        return get(id);
    }

    private void validateUpdateRequest(Long id, TaskUpdateDTO dto)
    {
        ValidationUtil.validateId(id);
        ValidationUtil.validateNotNull(dto, "Task");
        if (dto.name() != null) validateName(dto.name());
        if (dto.minimumDurationInDays() != null) validateMinimumDuration(dto.minimumDurationInDays());
    }

    private void validateStatusChange(Task task, Task.TaskStatus status)
    {
        if (status == null) return;

        if (status != Task.TaskStatus.NOT_STARTED)
        {
            validateCanProgress(task);
        }
        if (status != Task.TaskStatus.DONE && task.getStatus() == Task.TaskStatus.DONE)
        {
            validateCanReopen(task);
        }
    }

    private void updateCompetence(Task task, TaskUpdateDTO dto)
    {
        if (dto.competenceId() == null)
        {
            if (dto.bestEstimate() != null || dto.likelyEstimate() != null || dto.worstEstimate() != null)
            {
                throw new BadRequestException("A competence id is required when updating a task estimate");
            }
            return;
        }

        if (dto.competenceId() == 0)
        {
            task.setCompetence(null, 0.0);
            return;
        }

        validateEstimate(dto.bestEstimate());
        validateEstimate(dto.likelyEstimate());
        validateEstimate(dto.worstEstimate());
        Competence competence = getExistingCompetence(dto.competenceId());
        validateCanBeAssigned(competence);
        validateAssignedEmployeesHaveCompetence(task, competence);
        task.setCompetence(competence, dto.bestEstimate(), dto.likelyEstimate(), dto.worstEstimate());
    }

    private void updateStatus(Task task, Task.TaskStatus status)
    {
        if (status == null) return;

        if (status == Task.TaskStatus.DONE && task.getStatus() != Task.TaskStatus.DONE)
        {
            WorkingDaySchedule.TaskDates dates = WorkingDaySchedule.calculateTaskDates(
                    task,
                    task.getStage().getProject().getStartDate());
            task.recordPlanningSnapshot(
                    dates.startDate(),
                    dates.endDate(),
                    task.getLaborDurationInDays(),
                    task.getScheduledDurationInDays());
        }
        task.changeStatus(status);
    }

    @Override
    public void delete(Long id)
    {
        getExistingTask(id);
        taskDAO.delete(id);
    }

    @Override
    public void addPredecessor(Long taskId, Long predecessorId)
    {
        ValidationUtil.validateId(taskId);
        ValidationUtil.validateId(predecessorId);

        Task task = getExistingTask(taskId);
        Task predecessor = getExistingTask(predecessorId);

        validateDependency(task, predecessor);

        if (task.getPredecessors().contains(predecessor))
        {
            throw new BadRequestException("Dependency already exists");
        }

        task.addPredecessor(predecessor);
        taskDAO.update(task);
    }

    @Override
    public void removePredecessor(Long taskId, Long predecessorId)
    {
        ValidationUtil.validateId(taskId);
        ValidationUtil.validateId(predecessorId);

        Task task = getExistingTask(taskId);
        Task predecessor = getExistingTask(predecessorId);

        if (!task.getPredecessors().contains(predecessor))
        {
            throw new BadRequestException("Dependency does not exist");
        }

        task.removePredecessor(predecessor);
        taskDAO.update(task);
    }

    @Override
    public TaskDTO replaceEmployees(Long taskId, TaskEmployeesDTO taskEmployeesDTO)
    {
        ValidationUtil.validateId(taskId);
        ValidationUtil.validateNotNull(taskEmployeesDTO, "Employees");
        ValidationUtil.validateNotNull(taskEmployeesDTO.employeeIds(), "Employee ids");
        taskEmployeesDTO.employeeIds().forEach(ValidationUtil::validateId);

        Task task = getExistingTask(taskId);
        List<Employee> employees = taskEmployeesDTO.employeeIds().stream()
                .distinct()
                .map(employeeReader::get)
                .toList();

        validateAssignmentsCanChange(task);
        employees.stream()
                .filter(employee -> !task.getEmployees().contains(employee))
                .forEach(employee -> validateNewAssignment(task, employee));

        task.replaceEmployees(employees);
        taskDAO.update(task);
        return get(taskId);
    }

    @Override
    public void assignEmployee(Long taskId, Long employeeId)
    {
        ValidationUtil.validateId(taskId);
        ValidationUtil.validateId(employeeId);

        Task task = getExistingTask(taskId);
        Employee employee = employeeReader.get(employeeId);

        if (task.getEmployees().contains(employee))
        {
            throw new BadRequestException("Employee is already assigned to the task");
        }

        validateAssignmentsCanChange(task);
        validateNewAssignment(task, employee);

        task.assignEmployee(employee);
        taskDAO.update(task);
    }

    @Override
    public void unassignEmployee(Long taskId, Long employeeId)
    {
        ValidationUtil.validateId(taskId);
        ValidationUtil.validateId(employeeId);

        Task task = getExistingTask(taskId);
        Employee employee = employeeReader.get(employeeId);

        if (!task.getEmployees().contains(employee))
        {
            throw new BadRequestException("Employee is not assigned to the task");
        }

        validateAssignmentsCanChange(task);

        task.unassignEmployee(employee);
        taskDAO.update(task);
    }

    private void validateDependency(Task task, Task predecessor)
    {
        if (!task.getStage().equals(predecessor.getStage()))
        {
            throw new BadRequestException("Task predecessors must belong to the same stage");
        }
        if (task.getStatus() != Task.TaskStatus.NOT_STARTED)
        {
            throw new BadRequestException("Cannot add predecessors to a task already started");
        }

        if (task.equals(predecessor))
        {
            throw new BadRequestException("Task cannot depend on itself");
        }

        if (task.wouldCreateCycleWith(predecessor))
        {
            throw new BadRequestException("Circular dependency detected");
        }
    }

    private void validateCanProgress(Task task)
    {
        if (task.getPredecessors().stream().anyMatch(p -> p.getStatus() != Task.TaskStatus.DONE)
                || task.getStage().getPredecessors().stream().anyMatch(p -> !p.isCompleted()))
        {
            throw new BadRequestException("Cannot start or complete task: unfinished task or stage predecessors");
        }
    }

    private void validateCanReopen(Task task)
    {
        if (stageReader.getAll().stream().anyMatch(s ->
                s.getPredecessors().contains(task.getStage()) && s.getTasks().stream().anyMatch(t ->
                        t.getStatus() != Task.TaskStatus.NOT_STARTED)))
        {
            throw new BadRequestException("Cannot reopen stage predecessor: dependent stage already started");
        }
        if (task.getStage().getTasks().stream().anyMatch(t ->
                t.getPredecessors().contains(task) && t.getStatus() != Task.TaskStatus.NOT_STARTED))
        {
            throw new BadRequestException("Cannot reopen predecessor: dependent tasks have started");
        }
    }

    private void validateName(String name)
    {
        ValidationUtil.validateNotBlank(name, "Task name");
    }

    private void validateMinimumDuration(Integer duration)
    {
        if (duration == null || duration < 0)
            throw new BadRequestException("Task minimum duration must be a non-negative number of days");
    }

    private void validateEstimate(Double estimate)
    {
        if (estimate == null || !Double.isFinite(estimate) || estimate < 0)
            throw new BadRequestException("Task estimate must be a non-negative number of hours");
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

    private void validateAssignmentsCanChange(Task task)
    {
        if (task.getStatus() == Task.TaskStatus.DONE)
        {
            throw new BadRequestException("Assignments cannot be changed on a completed task");
        }
    }

    private void validateNewAssignment(Task task, Employee employee)
    {
        if (!employee.isActive())
        {
            throw new BadRequestException("Inactive employees cannot be assigned");
        }

        Competence required = task.getCompetence();
        if (required != null && !employee.getCompetences().contains(required))
        {
            throw new BadRequestException("Employee does not have the task's required competence");
        }
    }

    private void validateAssignedEmployeesHaveCompetence(Task task, Competence competence)
    {
        boolean allAssignedHaveCompetence = task.getEmployees().stream()
                .allMatch(employee -> employee.getCompetences().contains(competence));

        if (!allAssignedHaveCompetence)
        {
            throw new BadRequestException(
                    "Unassign employees without the new competence before changing the task's competence");
        }
    }
}