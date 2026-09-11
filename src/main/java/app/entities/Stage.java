package app.entities;

import java.util.Set;
import java.util.HashSet;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;

@Entity
public class Stage implements IEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long id() { return id; }
    public String name;
    @ManyToOne(fetch = FetchType.LAZY)
    public Project project;
    //@OneToMany(mappedBy = "stage", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    //public Set<Task> tasks;

    public Stage() {}
    public Stage(String name, Project project)
    {
        this.name = name;
        this.project = project;
    }
}
