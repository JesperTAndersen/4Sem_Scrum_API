package app.task.data;

import app.exceptions.NotFoundException;
import app.shared.data.ICrudDAO;
import app.task.domain.Task;
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

public class TaskDAO implements ICrudDAO<Task>
{
    private final EntityManagerFactory emf;

    public TaskDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public Task create(Task task)
    {
        ValidationUtil.validateNotNull(task, "Task");

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                em.persist(task);
                em.getTransaction().commit();
                return task;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to create task", e);
            }
        }
    }

    @Override
    public Task get(Long id)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                Task task = em.createQuery(
                                """
                                SELECT DISTINCT t FROM Task t
                                LEFT JOIN FETCH t.requiredCompetences
                                WHERE t.id = :id
                                """,
                                Task.class)
                        .setParameter("id", id)
                        .getResultStream()
                        .findFirst()
                        .orElse(null);
                return DBValidator.validateExists(task, id, Task.class);
            }
            catch (EntityNotFoundException e)
            {
                throw new NotFoundException("No task found with id: " + id);
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch task by id: " + id, e);
            }
        }
    }

    @Override
    public List<Task> getAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                TypedQuery<Task> query = em.createQuery(
                        """
                        SELECT DISTINCT t FROM Task t
                        LEFT JOIN FETCH t.requiredCompetences
                        ORDER BY t.id
                        """,
                        Task.class);
                return query.getResultList();
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch all tasks", e);
            }
        }
    }

    @Override
    public Task update(Task task)
    {
        ValidationUtil.validateNotNull(task, "Task");
        ValidationUtil.validateId(task.getId());

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                DBValidator.validateExists(em.find(Task.class, task.getId()), task.getId(), Task.class);
                Task merged = em.merge(task);
                em.getTransaction().commit();
                return merged;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("No task found with id: " + task.getId());
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to update task: " + task.getId(), e);
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
                Task managed = DBValidator.validateExists(em.find(Task.class, id), id, Task.class);
                em.remove(managed);
                em.getTransaction().commit();
                return true;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("No task found with id: " + id);
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to delete task: " + id, e);
            }
        }
    }
}
