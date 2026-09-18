package app.task.domain;

import app.shared.domain.IEntity;
import app.competence.domain.Competence;
import app.stage.domain.Stage;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double estimate;

    // Sprint-later fields:
    // private double duration;
    // private int crewSize;
    public enum TaskStatus
    {
        NOT_STARTED,
        IN_PROGESS,
        DONE,
        ;
    }

    private TaskStatus status;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_competences",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "competence_id")
    )
    private Set<Competence> requiredCompetences = new HashSet<>();
    // TODO: dependencies
    @ManyToOne(fetch = FetchType.LAZY)
    private Stage stage;
    // TODO: employees
    //@OneToMany(fetch = FetchType.LAZY)
    //private Set<Employee> assigned = new HashSet<>();

    public Task()
    {
    }

    public Task(Stage stage, String name, double estimate)
    {
        this(stage, name, estimate, Set.of());
    }

    public Task(Stage stage, String name, double estimate, Set<Competence> requiredCompetences)
    {
        this.stage = stage;
        this.name = name;
        this.estimate = estimate;
        this.requiredCompetences = new HashSet<>(requiredCompetences);
        this.status = TaskStatus.NOT_STARTED;
        if (stage != null)
        {
            stage.addTask(this);
        }
    }

    public void changeStatus(TaskStatus status)
    {
        this.status = status;
    }

    public void update(String name, double estimate, Set<Competence> requiredCompetences)
    {
        this.name = name;
        this.estimate = estimate;
        this.requiredCompetences = new HashSet<>(requiredCompetences);
    }

    // TODO: employees
    //public void assign(Employee employee)
    //{
    //    this.assigned.add(employee);
    //}

    //public void unassign(Employee employee)
    //{
    //    this.assigned.remove(employee);
    //}
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
        if (!(o instanceof Task)) return false;
        return id != null && id.equals(((Task) o).getId());
    }

    @Override
    public final int hashCode()
    {
        return getClass().hashCode();
    }
}
