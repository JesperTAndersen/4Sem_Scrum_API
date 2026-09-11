package app.config;

import app.config.hibernate.HibernateConfig;
import app.controllers.implementations.HealthCheckController;
import app.controllers.interfaces.IHealthCheckController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManagerFactory;
import lombok.Getter;

public final class DependencyContainer
{
    private static DependencyContainer instance;
    private final EntityManagerFactory entityManagerFactory;

    @Getter
    private final ObjectMapper objectMapper;
    @Getter
    private final IHealthCheckController healthCheckController;

    public DependencyContainer(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;

        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.healthCheckController = new HealthCheckController(entityManagerFactory);
    }

    public static DependencyContainer getInstance()
    {
        if (instance == null)
        {
            instance = new DependencyContainer(HibernateConfig.getEntityManagerFactory());
        }
        return instance;
    }

    public static DependencyContainer getTestInstance(EntityManagerFactory entityManagerFactory)
    {
        instance = new DependencyContainer(entityManagerFactory);
        return instance;
    }
}