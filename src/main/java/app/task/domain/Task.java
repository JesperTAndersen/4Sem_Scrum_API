package app.task.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import app.competence.domain.Competence;
import app.shared.domain.IEntity;
import app.stage.domain.Stage;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private double minDuration;

    // Sprint-later fields:
    // private double duration;
    // private int crewSize;
    public enum TaskStatus
    {
        NOT_STARTED,
        IN_PROGRESS,
        DONE,
        ;
    }

    private TaskStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    private Competence competence;
    private double estimate;

    // TODO: dependencies
    @ManyToOne(fetch = FetchType.LAZY)
    private Stage stage;
    // TODO: employees
    //@OneToMany(fetch = FetchType.LAZY)
    //private Set<Employee> assigned = new HashSet<>();

    public Task()
    {
    }

    public Task(Stage stage, String name, double minDuration)
    {
        this.stage = stage;
        this.name = name;
        this.minDuration = minDuration;
        this.status = TaskStatus.NOT_STARTED;
        if (stage != null) {
            if (stage != null) {
                stage.addTask(this);
            }
        }
    }

    public void changeStatus(TaskStatus status)
    {
        this.status = status;
    }

    public void setCompetence(Competence competence, float estimate)
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

    public void update(String name, Double minDuration)
    {
        if (name != null)
            this.name = name.trim();
        if (minDuration != null)
            this.minDuration = minDuration;
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