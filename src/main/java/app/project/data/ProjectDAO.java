package app.project.data;

import app.project.domain.Project;
import app.exceptions.DatabaseException;
import app.project.data.IProjectDAO;
import app.utils.DBValidator;
import app.utils.TransactionUtil;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class ProjectDAO implements IProjectDAO
{
    private final EntityManagerFactory emf;

    public ProjectDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public Project get(Long id)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                Project project = em.createQuery(
                                """
                                        SELECT DISTINCT p FROM Project p
                                        LEFT JOIN FETCH p.createdBy
                                        LEFT JOIN FETCH p.updatedBy
                                        LEFT JOIN FETCH p.stages s
                                        LEFT JOIN FETCH s.tasks
                                        WHERE p.id = :id""",

                                Project.class
                        )
                        .setParameter("id", id)
                        .getResultStream()
                        .findFirst()
                        .orElse(null);

                return DBValidator.validateExists(project, id, Project.class);
            }
            catch (EntityNotFoundException e)
            {
                throw e;
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch project by id: " + id, e);
            }
        }
    }

    @Override
    public List<Project> getAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                TypedQuery<Project> query = em.createQuery(
                        """ 
                                SELECT DISTINCT p FROM Project p 
                                LEFT JOIN FETCH p.createdBy
                                LEFT JOIN FETCH p.updatedBy
                                LEFT JOIN FETCH p.stages s
                                LEFT JOIN FETCH s.tasks 
                                ORDER BY p.id""",
                        Project.class
                );

                return query.getResultList();
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch all projects", e);
            }
        }
    }

    @Override
    public Project create(Project project)
    {
        ValidationUtil.validateNotNull(project, "Project");

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                em.persist(project);
                em.getTransaction().commit();
                return project;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to create project", e);
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

                Project managedProject = DBValidator.validateExists(
                        em.find(Project.class, id),
                        id,
                        Project.class
                );

                em.remove(managedProject);
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
                throw new DatabaseException("Failed to delete project: " + id, e);
            }
        }
    }

    @Override
    public Project update(Project project)
    {
        ValidationUtil.validateNotNull(project, "Project");
        ValidationUtil.validateId(project.getId());

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();

                DBValidator.validateExists(
                        em.find(Project.class, project.getId()),
                        project.getId(),
                        Project.class
                );

                Project updatedProject = em.merge(project);
                em.getTransaction().commit();
                return updatedProject;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw e;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to update project: " + project.getId(), e);
            }
        }
    }
}
