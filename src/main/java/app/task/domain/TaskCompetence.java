package app.task.domain;

import app.competence.domain.Competence;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Getter
@Entity
public class TaskCompetence
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private Task task;

    @ManyToOne(fetch = FetchType.EAGER)
    private Competence competence;

    private float estimate;

    public TaskCompetence() {}
    public TaskCompetence(Task task, Competence competence, float estimate)
    {
        this.task = task;
        this.competence = competence;
        this.estimate = estimate;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof TaskCompetence)) return false;
        TaskCompetence tc = (TaskCompetence)o;
        return task != null && tc.competence != null && task.equals(tc.task) && competence.equals(tc.competence);
    }

    @Override
    public final int hashCode()
    {
        return getClass().hashCode();
    }
}