package app.employee.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.competence.domain.Competence;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Employee
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    private double dailyCapacity;

    private boolean active;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Competence> competences = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Employee(String firstName, String lastName, double dailyCapacity)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dailyCapacity = dailyCapacity;
        this.active = true;
    }

    public void update(String firstName, String lastName, double dailyCapacity)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dailyCapacity = dailyCapacity;
    }

    public void setActive(boolean flag)
    {
        active = flag;
    }

    public void addCompetence(Competence comp)
    {
        competences.add(comp);
    }

    public void remCompetence(Competence comp)
    {
        competences.remove(comp);
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
        return this == o || (o instanceof Employee && id != null && id.equals(((Employee) o).getId()));
    }

    @Override
    public final int hashCode()
    {
        return getClass().hashCode();
    }
}