package app.dao;

import app.enums.UserRole;
import app.exceptions.DatabaseException;
import app.dao.IUserDAO;
import app.entities.User;
import app.utils.DBValidator;
import app.utils.TransactionUtil;
import app.utils.ValidationUtil;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class UserDAO implements IUserDAO
{
    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public User create(User user)
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
    public Set<User> getAll()
    {
        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
                return new HashSet<>(query.getResultList());
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed fetch all users", e);
            }
        }
    }

    @Override
    public User getByID(Long id)
    {
        ValidationUtil.validateId(id);

        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                User user = em.find(User.class, id);
                return DBValidator.validateExists(user, id, User.class);
            }
            catch (EntityNotFoundException e)
            {
                throw e;
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch user by id: " + id, e);
            }
        }
    }

    @Override
    public User update(User user)
    {
        ValidationUtil.validateNotNull(user, "User");
        ValidationUtil.validateId(user.getId());

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();
                User exist = em.find(User.class, user.getId());
                DBValidator.validateExists(exist, user.getId(), User.class);
                User merged = em.merge(user);
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
                throw new DatabaseException("Failed to update user: " + user.getId(), e);
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
                User managed = em.find(User.class, id);
                DBValidator.validateExists(managed, id, User.class);
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
                throw new DatabaseException("Failed to delete user: " + id, e);
            }
        }
    }

    @Override
    public Optional<User> findByEmail(String email)
    {
        ValidationUtil.validateNotNull(email, "Email");

        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

                return Optional.ofNullable(user);
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to fetch user by email: " + email, e);
            }
        }
    }

    @Override
    public Set<User> findByRole(UserRole role)
    {
        ValidationUtil.validateNotNull(role, "UserRole");

        try(EntityManager em = emf.createEntityManager())
        {
            try
            {
                TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.userRole = :role", User.class)
                    .setParameter("role", role);
                return new HashSet<>(query.getResultList());
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to find user by role: " + role, e);
            }
        }
    }

    @Override
    public boolean existsByEmail(String email)
    {
        ValidationUtil.validateNotNull(email, "Email");

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
                return count > 0;
            }
            catch (PersistenceException e)
            {
                throw new DatabaseException("Failed to check if user exists by email: " + email, e);
            }
        }
    }
}
