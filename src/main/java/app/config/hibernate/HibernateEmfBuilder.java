package app.config.hibernate;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

// This class is just a factory that converts properties into an EntityManagerFactory

public final class HibernateEmfBuilder
{
    private HibernateEmfBuilder()
    {
    }

    public static EntityManagerFactory build(Properties props)
    {
        try
        {
            Configuration configuration = new Configuration();
            configuration.setProperties(props);

            // Register entities to make Hibernate aware
            EntityRegistry.registerEntities(configuration);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            // Make JPA compliant version of Hibernates sf
            SessionFactory sf = configuration.buildSessionFactory(serviceRegistry);
            return sf.unwrap(EntityManagerFactory.class);

        }
        catch (Throwable ex)
        {
            System.err.println("Initial SessionFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
}