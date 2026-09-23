package app.competence.data;

import app.competence.domain.Competence;
import app.exceptions.DatabaseException;
import app.exceptions.NotFoundException;
import app.utils.DBValidator;
import app.utils.TransactionUtil;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class CompetenceDAO implements ICompetenceDAO
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
                throw new NotFoundException("No competence found with id: " + id);
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
                Competence updatedCompetence = DBValidator.validateExists(
                        em.find(Competence.class, competence.getId()), competence.getId(), Competence.class);
                updatedCompetence.update(competence.getName(), competence.getRate());
                em.getTransaction().commit();
                return updatedCompetence;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("No competence found with id: " + competence.getId());
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
                throw new NotFoundException("No competence found with id: " + id);
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to delete competence: " + id, e);
            }
        }
    }

    @Override
    public void setActive(Long id, boolean active)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                Competence competence = DBValidator.validateExists(em.find(Competence.class, id), id, Competence.class);
                competence.setActive(active);
                em.getTransaction().commit();
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("No competence found with id: " + id);
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to change competence active state: " + id, e);
            }
        }
    }

    @Override
    public boolean existsByName(String name, Long excludedId)
    {
        ValidationUtil.validateNotBlank(name, "Competence name");

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                String query = "SELECT COUNT(c) FROM Competence c WHERE LOWER(c.name) = LOWER(:name)";
                if (excludedId != null)
                {
                    query += " AND c.id <> :excludedId";
                }

                var typedQuery = em.createQuery(query, Long.class)
                        .setParameter("name", name.trim());
                if (excludedId != null)
                {
                    typedQuery.setParameter("excludedId", excludedId);
                }
                return typedQuery.getSingleResult() > 0;
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to check competence name", e);
            }
        }
    }

    @Override
    public boolean isInUse(Long id)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                Long count = em.createQuery("""
                        SELECT COUNT(t)
                        FROM Task t JOIN t.competence c
                        WHERE c.id = :id
                        """, Long.class)
                        .setParameter("id", id)
                        .getSingleResult();
                return count > 0;
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to check competence usage: " + id, e);
            }
        }
    }
}
