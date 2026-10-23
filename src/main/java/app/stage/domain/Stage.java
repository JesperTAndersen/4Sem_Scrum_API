package app.stage.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import app.project.domain.Project;
import app.shared.domain.IEntity;
import app.task.domain.Task;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

@Getter
@Entity
public class Stage implements IEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @OneToMany(mappedBy = "stage", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Task> tasks = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "stage_predecessors", joinColumns = @JoinColumn(name = "stage_id"),
            inverseJoinColumns = @JoinColumn(name = "predecessor_id"))
    private Set<Stage> predecessors = new HashSet<>();

    public boolean isCompleted()
    {
        return !tasks.isEmpty() && tasks.stream().allMatch(t -> t.getStatus() == Task.TaskStatus.DONE);
    }

    public boolean wouldCreateCycleWith(Stage predecessor)
    {
        return predecessor.canReach(this, new HashSet<>());
    }

    private boolean canReach(Stage target, Set<Stage> visited)
    {
        if (this == target || (id != null && id.equals(target.id))) return true;
        if (!visited.add(this)) return false;
        return predecessors.stream().anyMatch(p -> p.canReach(target, visited));
    }

    public Stage() {}
    public Stage(String name, Project project)
    {
        this.name = name;
        this.project = project;
    }

    public void update(String name)
    {
        this.name = name;
    }

    public void addTask(Task task)
    {
        if (task != null)
        {
            tasks.add(task);
        }
    }

    public double getTotalEstimatedHours()
    {
        return tasks.stream()
                .mapToDouble(Task::calcEstimate)
                .sum();
    }

    public BigDecimal getTotalCost()
    {
        BigDecimal total = new BigDecimal(0);
        for (Task task : tasks) {
            total = total.add(task.getCost());
        }
        return total;
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate()
    {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate()
    {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof Stage)) return false;
        return id != null && id.equals(((Stage)o).getId());
    }

    @Override
    public final int hashCode()
    {
        return getClass().hashCode();
    }
}