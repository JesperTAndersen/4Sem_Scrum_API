package app.persistence.implementations;

import app.persistence.interfaces.specific.IStageDAO;
import app.entities.Stage;
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

public class StageDAO implements IStageDAO
{
    private final EntityManagerFactory emf;

    public StageDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public Stage create(Stage stage)
    {
        ValidationUtil.validateNotNull(stage, "Stage");

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                em.persist(stage);
                em.getTransaction().commit();
                return stage;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to create stage", e);
            }
        }
    }

    @Override
    public Stage get(Long id)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                return DBValidator.validateExists(em.find(Stage.class, id), id, Stage.class);
            }
            catch (EntityNotFoundException e)
            {
                throw e;
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch stage by id: " + id, e);
            }
        }
    }

    @Override
    public List<Stage> getAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                TypedQuery<Stage> query = em.createQuery("SELECT s FROM Stage s ORDER BY s.id", Stage.class);
                return query.getResultList();
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch all stages", e);
            }
        }
    }

    @Override
    public Stage update(Stage stage)
    {
        ValidationUtil.validateNotNull(stage, "Stage");
        ValidationUtil.validateId(stage.id());

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                DBValidator.validateExists(em.find(Stage.class, stage.id()), stage.id(), Stage.class);
                Stage merged = em.merge(stage);
                em.getTransaction().commit();
                return merged;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw e;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to update stage: " + stage.id(), e);
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
                Stage managed = DBValidator.validateExists(em.find(Stage.class, id), id, Stage.class);
                em.remove(managed);
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
                throw new DatabaseException("Failed to delete stage: " + id, e);
            }
        }
    }
}
