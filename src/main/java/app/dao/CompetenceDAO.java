package app.dao;

import app.entities.Competence;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class CompetenceDAO implements ICrudDAO<Competence>
{
    private final EntityManagerFactory emf;

    public CompetenceDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public Competence create(Competence competence)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.persist(competence);
            em.getTransaction().commit();
            return competence;
        }
    }

    @Override
    public Competence get(Long id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.find(Competence.class, id);
        }
    }

    @Override
    public List<Competence> getAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT c FROM Competence c ORDER BY c.id", Competence.class)
                    .getResultList();
        }
    }

    @Override
    public Competence update(Competence competence)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            Competence updatedCompetence = em.merge(competence);
            em.getTransaction().commit();
            return updatedCompetence;
        }
    }

    @Override
    public Competence delete(Competence competence)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            Competence managedCompetence = em.merge(competence);
            em.remove(managedCompetence);
            em.getTransaction().commit();
            return managedCompetence;
        }
    }
}
