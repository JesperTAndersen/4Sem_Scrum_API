package app.entities;

import java.time.LocalDateTime;

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
public class Stage implements IEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;
    //@OneToMany(mappedBy = "stage", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    //public Set<Task> tasks;

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
        return id.hashCode();
    }
}
