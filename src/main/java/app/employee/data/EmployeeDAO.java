package app.employee.data;

import java.util.List;

import app.employee.domain.Employee;
import app.exceptions.DatabaseException;
import app.exceptions.NotFoundException;
import app.utils.DBValidator;
import app.utils.TransactionUtil;
import app.utils.ValidationUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class EmployeeDAO implements IEmployeeDAO
{
    private final EntityManagerFactory emf;

    public EmployeeDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public Employee create(Employee employee)
    {
        ValidationUtil.validateNotNull(employee, "Employee");

        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                em.persist(employee);
                em.getTransaction().commit();
                return employee;
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to create employee", e);
            }
        }
    }

    @Override
    public Employee get(Long id)
    {
        ValidationUtil.validateId(id);

        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                Employee employee = em.find(Employee.class, id);
                return DBValidator.validateExists(employee, id, Employee.class);
            }
            catch (EntityNotFoundException e)
            {
                throw new NotFoundException("No employee found with id: " + id);
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch employee by id: " + id, e);
            }
        }
    }

    @Override
    public List<Employee> getAll()
    {
        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                TypedQuery<Employee> query = em.createQuery("SELECT e FROM Employee e", Employee.class);
                return query.getResultList();
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed fetch all Employees", e);
            }
        }
    }

    @Override
    public Employee update(Employee employee)
    {
        ValidationUtil.validateNotNull(employee, "Employee");
        ValidationUtil.validateId(employee.getId());

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                Employee exist = em.find(Employee.class, employee.getId());
                DBValidator.validateExists(exist, employee.getId(), Employee.class);
                Employee merged = em.merge(employee);
                em.getTransaction().commit();
                return merged;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("No employee found with id: " + employee.getId());
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to update employee: " + employee.getId(), e);
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
                Employee managed = em.find(Employee.class, id);
                DBValidator.validateExists(managed, id, Employee.class);
                em.remove(managed);
                em.getTransaction().commit();
                return true;
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("No employee found with id: " + id);
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to delete employee: " + id, e);
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
                Query query = em.createQuery("""
                    UPDATE Employee e SET e.active = :active WHERE e.id=:id
                        """);
                query.setParameter("active", active);
                query.setParameter("id", id);
                em.getTransaction().begin();
                query.executeUpdate();
                em.getTransaction().commit();
            }
            catch (EntityNotFoundException e)
            {
                TransactionUtil.rollback(em);
                throw new NotFoundException("No employee found with id: " + id);
            }
            catch (PersistenceException e)
            {
                TransactionUtil.rollback(em);
                throw new DatabaseException("Failed to set active employee: " + id, e);
            }
        }
    }
}