package app.capacity.data;

import app.capacity.domain.CompanyCapacity;
import app.exceptions.DatabaseException;
import app.exceptions.NotFoundException;
import app.utils.DBValidator;
import app.utils.TransactionUtil;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

public class CompanyCapacityDAO implements ICompanyCapacityDAO
{
    private final EntityManagerFactory emf;

    public CompanyCapacityDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public void initialize()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                em.createNativeQuery("""
                                INSERT INTO CompanyCapacity (id, dailyCapacity) VALUES (:id, :capacity)
                                ON CONFLICT (id) DO NOTHING
                                """)
                        .setParameter("id", CompanyCapacity.STANDARD_ID)
                        .setParameter("capacity", CompanyCapacity.DEFAULT_DAILY_CAPACITY)
                        .executeUpdate();
                em.getTransaction().commit();
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to initialize company capacity", e);
            }
        }
    }

    @Override
    public CompanyCapacity get()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                CompanyCapacity companyCapacity = em.find(CompanyCapacity.class, CompanyCapacity.STANDARD_ID);
                return DBValidator.validateExists(companyCapacity, CompanyCapacity.STANDARD_ID, CompanyCapacity.class);
            }
            catch (EntityNotFoundException e)
            {
                throw new NotFoundException("Company capacity has not been initialized");
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch company capacity", e);
            }
        }
    }

    @Override
    public CompanyCapacity update(CompanyCapacity companyCapacity)
    {
        ValidationUtil.validateNotNull(companyCapacity, "Company capacity");

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                DBValidator.validateExists(
                        em.find(CompanyCapacity.class, companyCapacity.getId()),
                        companyCapacity.getId(),
                        CompanyCapacity.class);
                CompanyCapacity merged = em.merge(companyCapacity);
                em.getTransaction().commit();
                return merged;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("Company capacity has not been initialized");
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to update company capacity", e);
            }
        }
    }
}