package app.controllers.implementations;

import app.controllers.interfaces.IHealthCheckController;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthCheckController implements IHealthCheckController
{
    private final EntityManagerFactory emf;

    public HealthCheckController(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    @Override
    public void healthCheck(Context ctx)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.createNativeQuery("SELECT 1").getSingleResult();
            ctx.status(HttpStatus.OK).json("{\"status\": \"ok\"}");
        }
        catch (Exception e)
        {
            log.warn("Database health check failed: {}", e.getMessage());
            ctx.status(HttpStatus.SERVICE_UNAVAILABLE).json("{\"status\": \"unavailable\"}");
        }
    }
}
