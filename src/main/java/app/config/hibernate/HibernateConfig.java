package app.config.hibernate;

import app.utils.PropertyReader;
import jakarta.persistence.EntityManagerFactory;

import java.util.Properties;

public final class HibernateConfig
{
    private static volatile EntityManagerFactory emf;

    private HibernateConfig()
    {
    }

    public static EntityManagerFactory getEntityManagerFactory()
    {
        if (emf == null)
        {
            synchronized (HibernateConfig.class)
            {
                if (emf == null)
                {
                    emf = HibernateEmfBuilder.build(buildProps());
                }
            }
        }
        return emf;
    }

    private static Properties buildProps()
    {
        Properties props = HibernateBaseProperties.createBase();

        // Teaching-friendly default - change to update in production
        props.put("hibernate.hbm2ddl.auto", "update");

        if (System.getenv("DEPLOYED") != null)
        {
            setDeployedProperties(props);
        }
        else
        {
            setDevProperties(props);
        }
        return props;
    }

    private static void setDeployedProperties(Properties props)
    {
        String dbName = System.getenv("DB_NAME");
        props.setProperty("hibernate.connection.url", System.getenv("CONNECTION_STR"));
        props.setProperty("hibernate.connection.username", System.getenv("DB_USERNAME"));
        props.setProperty("hibernate.connection.password", System.getenv("DB_PASSWORD"));
    }

    private static void setDevProperties(Properties props)
    {
        String dbName = PropertyReader.getPropertyValue("DB_NAME", "config.properties");
        String username = PropertyReader.getPropertyValue("DB_USERNAME", "config.properties");
        String password = PropertyReader.getPropertyValue("DB_PASSWORD", "config.properties");

        props.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/" + dbName);
        props.put("hibernate.connection.username", username);
        props.put("hibernate.connection.password", password);
    }
}