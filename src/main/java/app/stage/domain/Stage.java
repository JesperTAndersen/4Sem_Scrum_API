package app.stage.domain;

import app.shared.domain.IEntity;
import app.project.domain.Project;
import app.task.domain.Task;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
public class Stage implements IEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @OneToMany(mappedBy = "stage", fetch = FetchType.LAZY)
    private Set<Task> tasks = new HashSet<>();

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
