package app.persistence.implementations;

import app.persistence.interfaces.generic.ICrudDAO;
import app.entities.Competence;
import app.exceptions.DatabaseException;
import app.utils.DBValidator;
import app.utils.TransactionUtil;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

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
        ValidationUtil.validateNotNull(competence, "Competence");

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                em.persist(competence);
                em.getTransaction().commit();
                return competence;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to create competence", e);
            }
        }
    }

    @Override
    public Competence get(Long id)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                return DBValidator.validateExists(em.find(Competence.class, id), id, Competence.class);
            }
            catch (EntityNotFoundException e)
            {
                throw e;
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch competence by id: " + id, e);
            }
        }
    }

    @Override
    public List<Competence> getAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                TypedQuery<Competence> query = em.createQuery("SELECT c FROM Competence c ORDER BY c.id", Competence.class);
                return query.getResultList();
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch all competences", e);
            }
        }
    }

    @Override
    public Competence update(Competence competence)
    {
        ValidationUtil.validateNotNull(competence, "Competence");
        ValidationUtil.validateId(competence.getId());

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                DBValidator.validateExists(em.find(Competence.class, competence.getId()), competence.getId(), Competence.class);
                Competence updatedCompetence = em.merge(competence);
                em.getTransaction().commit();
                return updatedCompetence;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw e;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to update competence: " + competence.getId(), e);
            }
        }
    }

    @Override
    public boolean delete(Long id)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                Competence managedCompetence = DBValidator.validateExists(
                        em.find(Competence.class, id), id, Competence.class);
                em.remove(managedCompetence);
                em.getTransaction().commit();
                return true;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw e;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to delete competence: " + id, e);
            }
        }
    }
}
