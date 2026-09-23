package app.task.domain;

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

import java.time.LocalDateTime;

@Getter
@Entity
public class Task implements IEntity
{
    private static final double WORKING_HOURS_PER_DAY = 7.5;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int minimumDurationInDays;
    private double estimate;

    public enum TaskStatus { NOT_STARTED, IN_PROGRESS, DONE }
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    private Competence competence;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stage stage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public double getLaborDurationInDays() { return estimate / WORKING_HOURS_PER_DAY; }

    public double getScheduledDurationInDays()
    {
        return Math.max(getLaborDurationInDays(), minimumDurationInDays);
    }

    @PrePersist protected void onCreate()
    {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Override public final boolean equals(Object o)
    {
        return this == o || (o instanceof Task && id != null && id.equals(((Task) o).getId()));
    }

    @Override public final int hashCode() { return getClass().hashCode(); }
}
