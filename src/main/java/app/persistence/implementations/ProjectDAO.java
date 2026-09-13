package app.persistence.implementations;

import app.entities.Project;
import app.exceptions.DatabaseException;
import app.persistence.interfaces.generic.ICrudDAO;
import app.utils.DBValidator;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

import java.util.List;

public class ProjectDAO implements ICrudDAO<Project>
{
    private final EntityManagerFactory emf;

    public ProjectDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    public Project get(Long id)
    {
        ValidationUtil.validateId(id);

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                return DBValidator.validateExists(em.find(Project.class, id), id, Project.class);
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
        return List.of();
    }

    @Override
    public Project create(Project project)
    {
        return null;
    }

    @Override
    public boolean delete(Long id)
    {
        return false;
    }

    @Override
    public Project update(Project project)
    {
        return null;
    }
}
