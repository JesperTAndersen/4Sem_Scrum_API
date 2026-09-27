package app.task.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import app.competence.domain.Competence;
import app.shared.domain.IEntity;
import app.stage.domain.Stage;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class Task implements IEntity
{
    private static final double WORKING_HOURS_PER_DAY = 7.5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int minimumDurationInDays;

    public enum TaskStatus { NOT_STARTED, IN_PROGRESS, DONE }

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    private Competence competence;
    private double estimate;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stage stage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "task_predecessors",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "predecessor_id")
    )
    private Set<Task> predecessors = new HashSet<>();

    public Task() {}

    public Task(Stage stage, String name, int minimumDurationInDays)
    {
        this.stage = stage;
        this.name = name;
        this.minimumDurationInDays = minimumDurationInDays;
        this.status = TaskStatus.NOT_STARTED;
        if (stage != null) stage.addTask(this);
    }

    public void changeStatus(TaskStatus status) { this.status = status; }

    public void update(String name, Integer minimumDurationInDays)
    {
        if (name != null) this.name = name.trim();
        if (minimumDurationInDays != null) this.minimumDurationInDays = minimumDurationInDays;
    }

    public void setCompetence(Competence competence, double estimate)
    {
        this.competence = competence;
        this.estimate = estimate;
    }

    public BigDecimal getCost()
    {
        if (competence != null) {
            return competence.getRate().multiply(new BigDecimal(estimate));
        }
        return new BigDecimal(0);
    }

    public double getLaborDurationInDays()
    {
        return estimate / WORKING_HOURS_PER_DAY;
    }

    public double getScheduledDurationInDays()
    {
        return Math.max(getLaborDurationInDays(), minimumDurationInDays);
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

    public double getDependencyStartOffsetInDays()
    {
        double latestPredecessorFinish = 0;
        for (Task predecessor : predecessors)
        {
            double predecessorFinish = predecessor.getDependencyFinishOffsetInDays();
            if (predecessorFinish > latestPredecessorFinish)
            {
                latestPredecessorFinish = predecessorFinish;
            }
        }
        return latestPredecessorFinish;
    }

    public double getDependencyFinishOffsetInDays()
    {
        return getDependencyStartOffsetInDays() + getScheduledDurationInDays();
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

    @Override public final boolean equals(Object o)
    {
        return this == o || (o instanceof Task && id != null && id.equals(((Task) o).getId()));
    }

    @Override
    public final int hashCode()
    {
        return getClass().hashCode();
    }
}
