package app.employee.data;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

public class EmployeeDAO implements IEmployeeDAO
{
    private final EntityManagerFactory emf;

    public EmployeeDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }
    @Override
    public Object create(Object o)
    {
        ValidationUtil.validateNotNull(user, "User");

        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                em.persist(user);
                em.getTransaction().commit();
                return user;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to create user", e);
            }
        }
    }

    @Override
    public Object get(Long id)
    {
        return null;
    }

    @Override
    public List getAll()
    {
        return List.of();
    }

    @Override
    public Object update(Object o)
    {
        return null;
    }

    @Override
    public boolean delete(Long id)
    {
        return false;
    }

    @Override
    public void setActive(Long id, boolean active)
    {

    }
}