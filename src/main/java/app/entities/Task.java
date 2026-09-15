package app.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
    private float estimate; /* in hours */
    private float duration; /* in hours */
    private int crewSize;
    public static enum TaskStatus
    {
        NOT_STARTED,
        IN_PROGESS,
        DONE,
        ;
    }
    private TaskStatus status;
    @ManyToOne
    private Competence competency;
    // TODO: dependencies
    @ManyToOne(fetch = FetchType.LAZY)
    private Stage stage;
    // TODO: employees
    //@OneToMany(fetch = FetchType.LAZY)
    //private Set<Employee> assigned = new HashSet<>();

    public Task() {}
    public Task(Stage stage, String name, float estimate, float duration, int crewSize)
    {
        this.stage = stage;
        this.name = name;
        this.estimate = estimate;
        this.duration = duration;
        this.crewSize = crewSize;
        this.status = TaskStatus.NOT_STARTED;
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
        return id != null && id.equals(((Task)o).getId());
    }

    @Override
    public final int hashCode()
    {
        return id.hashCode();
    }
}
