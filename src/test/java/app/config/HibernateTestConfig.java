package app.config;

import app.config.hibernate.HibernateBaseProperties;
import app.config.hibernate.HibernateEmfBuilder;
import jakarta.persistence.EntityManagerFactory;

import java.util.Properties;

public final class HibernateTestConfig
{
    private static volatile EntityManagerFactory emf;

    private HibernateTestConfig()
    {
    }

    public static EntityManagerFactory getEntityManagerFactory()
    {
        if (emf == null || !emf.isOpen())
        {
            synchronized (HibernateTestConfig.class)
            {
                if (emf == null || !emf.isOpen())
                {
                    emf = HibernateEmfBuilder.build(buildProperties());
                }
            }
        }
        return emf;
    }

    private static Properties buildProperties()
    {
        Properties properties = HibernateBaseProperties.createBase();
        properties.put("hibernate.connection.driver_class", "org.testcontainers.jdbc.ContainerDatabaseDriver");
        properties.put("hibernate.connection.url", "jdbc:tc:postgresql:16.2:///scrum_api_test");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        return properties;
    }
}
