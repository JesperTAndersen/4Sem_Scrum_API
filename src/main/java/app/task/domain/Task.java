package app.task.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.competence.domain.Competence;
import app.employee.domain.Employee;
import app.shared.domain.IEntity;
import app.stage.domain.Stage;
import app.task.presentation.dto.StaffingWarning;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

@Getter
@Entity
public class Task implements IEntity
{
    private static final double ASSUMED_WORKING_HOURS_PER_DAY = 7.5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int minimumDurationInDays;

    public enum TaskStatus
    {NOT_STARTED, IN_PROGRESS, DONE}

    public enum StaffingStatus
    {STAFFED, STAFFING_REQUIRED, NO_AVAILABLE_CAPACITY}

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    private Competence competence;
    private double bestEstimate;
    private double likelyEstimate;
    private double worstEstimate;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stage stage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDate completedStartDate;
    private LocalDate completedEndDate;
    private Double completedLaborDurationInDays;
    private Double completedScheduledDurationInDays;
    private LocalDateTime planningSnapshotRecordedAt;

    public void recordPlanningSnapshot(
            LocalDate startDate,
            LocalDate endDate,
            Double laborDurationInDays,
            Double scheduledDurationInDays)
    {
        this.completedStartDate = startDate;
        this.completedEndDate = endDate;
        this.completedLaborDurationInDays = laborDurationInDays;
        this.completedScheduledDurationInDays = scheduledDurationInDays;
        this.planningSnapshotRecordedAt = LocalDateTime.now();
    }

    public boolean hasPlanningSnapshot()
    {
        return planningSnapshotRecordedAt != null;
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "task_predecessors",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "predecessor_id")
    )
    private Set<Task> predecessors = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Employee> employees = new HashSet<>();

    public Task()
    {
    }

    public Task(Stage stage, String name, int minimumDurationInDays)
    {
        this.stage = stage;
        this.name = name;
        this.minimumDurationInDays = minimumDurationInDays;
        this.status = TaskStatus.NOT_STARTED;
        if (stage != null) stage.addTask(this);
    }

    public void changeStatus(TaskStatus status)
    {
        this.status = status;
    }

    public void update(String name, Integer minimumDurationInDays)
    {
        if (name != null) this.name = name.trim();
        if (minimumDurationInDays != null) this.minimumDurationInDays = minimumDurationInDays;
    }

    public void setCompetence(Competence competence, double estimate)
    {
        this.competence = competence;
        this.bestEstimate = estimate;
        this.likelyEstimate = estimate;
        this.worstEstimate = estimate;
    }

    public void setCompetence(Competence competence, double best, double likely, double worst)
    {
        this.competence = competence;
        this.bestEstimate = best;
        this.likelyEstimate = likely;
        this.worstEstimate = worst;
    }

    public double calcEstimate()
    {
        return Math.round((bestEstimate + 4*likelyEstimate + worstEstimate) / 6);
    }

    public BigDecimal getCost()
    {
        if (competence != null)
        {
            return competence.getRate().multiply(new BigDecimal(calcEstimate()));
        }
        return new BigDecimal(0);
    }

    public Double getLaborDurationInDays()
    {
        if (status == TaskStatus.DONE && hasPlanningSnapshot())
        {
            return this.completedLaborDurationInDays;
        }

        return switch (getStaffingStatus())
        {
            case STAFFED -> calcEstimate() / getAvailableDailyCapacity();
            case STAFFING_REQUIRED -> calcEstimate() / ASSUMED_WORKING_HOURS_PER_DAY;
            case NO_AVAILABLE_CAPACITY -> null;
        };
    }

    public Double getScheduledDurationInDays()
    {
        if (status == TaskStatus.DONE && hasPlanningSnapshot())
        {
            return this.completedScheduledDurationInDays;
        }

        Double labor = getLaborDurationInDays();
        return labor == null ? null : Math.max(labor, minimumDurationInDays);
    }

    public void addPredecessor(Task predecessor)
    {
        if (predecessor != null)
        {
            predecessors.add(predecessor);
        }
    }

    public void removePredecessor(Task predecessor)
    {
        predecessors.remove(predecessor);
    }

    public boolean wouldCreateCycleWith(Task potentialPredecessor)
    {
        Set<Task> visited = new HashSet<>();
        return potentialPredecessor.canReach(this, visited);
    }

    private boolean canReach(Task target, Set<Task> visited)
    {
        if (this.equals(target))
        {
            return true;
        }
        if (visited.contains(this))
        {
            return false;
        }

        visited.add(this);
        for (Task predecessor : predecessors)
        {
            if (predecessor.canReach(target, visited))
            {
                return true;
            }
        }
        return false;
    }

    public Double getDependencyStartOffsetInDays()
    {
        double latestPredecessorFinish = 0;
        for (Task predecessor : getAllPredecessorTasks())
        {
            Double predecessorFinish = predecessor.getDependencyFinishOffsetInDays();
            if (predecessorFinish == null)
            {
                return null;
            }
            latestPredecessorFinish = Math.max(latestPredecessorFinish, predecessorFinish);
        }
        return latestPredecessorFinish;
    }

    public Double getDependencyFinishOffsetInDays()
    {
        Double start = getDependencyStartOffsetInDays();
        Double duration = getScheduledDurationInDays();
        if (start == null || duration == null)
        {
            return null;
        }
        return start + Math.ceil(duration);
    }

    private List<Task> getAllPredecessorTasks()
    {
        List<Task> allPredecessors = new ArrayList<>(predecessors);
        if (stage != null)
        {
            for (Stage predecessorStage : stage.getPredecessors())
            {
                allPredecessors.addAll(predecessorStage.getTasks());
            }
        }
        return allPredecessors;
    }

    public void assignEmployee(Employee employee)
    {
        validateEmployeeNotNull(employee);
        employees.add(employee);
    }

    public void unassignEmployee(Employee employee)
    {
        validateEmployeeNotNull(employee);
        employees.remove(employee);
    }

    public void replaceEmployees(List<Employee> employees)
    {
        employees.forEach(this::validateEmployeeNotNull);
        this.employees.clear();
        this.employees.addAll(employees);
    }

    private double getAvailableDailyCapacity()
    {
        return employees.stream()
                .filter(employee -> status == TaskStatus.DONE || (employee.isActive() && isEligible(employee)))
                .mapToDouble(Employee::getDailyCapacity)
                .sum();
    }

    private boolean isEligible(Employee employee)
    {
        return competence == null || employee.getCompetences().contains(competence);
    }

    public List<StaffingWarning> getStaffingWarnings()
    {
        if (status == TaskStatus.DONE) return List.of();

        return employees.stream()
                .filter(employee -> !employee.isActive() || !isEligible(employee))
                .sorted(Comparator.comparing(Employee::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(employee -> new StaffingWarning(
                        employee.isActive() ?
                                StaffingWarning.WarningType.ASSIGNED_EMPLOYEE_MISSING_COMPETENCE
                                : StaffingWarning.WarningType.INACTIVE_ASSIGNED_EMPLOYEE,
                        id, employee.getId()))
                .toList();
    }

    public StaffingStatus getStaffingStatus()
    {
        if (employees.isEmpty())
        {
            return StaffingStatus.STAFFING_REQUIRED;
        }

        return getAvailableDailyCapacity() > 0 ? StaffingStatus.STAFFED : StaffingStatus.NO_AVAILABLE_CAPACITY;
    }

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate()
    {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o)
    {
        return this == o || (o instanceof Task && id != null && id.equals(((Task) o).getId()));
    }

    @Override
    public final int hashCode()
    {
        return getClass().hashCode();
    }

    private void validateEmployeeNotNull(Employee employee)
    {
        if (employee == null)
        {
            throw new IllegalArgumentException("Employee cant be null");
        }
    }
}