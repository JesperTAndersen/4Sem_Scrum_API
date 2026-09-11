package app.persistence.implementations;

import app.entities.Project;
import app.exceptions.DatabaseException;
import app.utils.DBValidator;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

public class ProjectDAO
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
}
