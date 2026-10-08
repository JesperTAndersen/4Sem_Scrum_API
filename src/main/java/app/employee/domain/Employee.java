package app.employee.domain;

import app.capacity.domain.CompanyCapacity;
import app.competence.domain.Competence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "usesStandardCapacity", nullable = false, columnDefinition = "boolean default false")
    private boolean standardCapacity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "standardCapacity_id")
    private CompanyCapacity companyCapacity;

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

    public double getDailyCapacity()
    {
        return standardCapacity ? companyCapacity.getDailyCapacity() : dailyCapacity;
    }

    public void configureCapacity(boolean standardCapacity, CompanyCapacity companyCapacity)
    {
        this.standardCapacity = standardCapacity;
        this.companyCapacity = companyCapacity;
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
        if (!competences.contains(comp)) competences.add(comp);
    }

    public void replaceCompetences(List<Competence> competences)
    {
        this.competences.clear();
        competences.forEach(this::addCompetence);
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