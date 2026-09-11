package app.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

abstract class Crud<T>
{
    Class<T> clazz;
    EntityManagerFactory emf;

    @SuppressWarnings("unchecked")
    Crud(EntityManagerFactory emf)
    {
        this.emf = emf;
        Type type = ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.clazz = (Class<T>)type;
    }

    public void create(T t)
    {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(t);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public T read(Long id)
    {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(clazz, id);
        } catch (Exception e) {
            throw e;
        }
    }

    public void update(T t)
    {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(t);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(T t)
    {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(t);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
