package app.presentation.health;

import app.config.HibernateTestConfig;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.lang.reflect.Proxy;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class HealthCheckControllerTest
{
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/health/health-check-controller/check-database.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Health check - reports whether the database is available")
    void reportsDatabaseAvailability(boolean databaseAvailable, int expected, String reason)
    {
        EntityManagerFactory emf = databaseAvailable
                ? HibernateTestConfig.getEntityManagerFactory()
                : unavailableDatabase();
        HealthCheckController controller = new HealthCheckController(emf);
        Javalin app = Javalin.create(config -> config.routes.apiBuilder(() -> get("/health", controller::healthCheck)));
        app.start(0);

        try
        {
            given().baseUri("http://localhost").basePath("").port(app.port())
                    .when().get("/health")
                    .then().statusCode(expected)
                    .body("status", is(databaseAvailable ? "ok" : "unavailable"));
        }
        finally
        {
            app.stop();
        }
    }

    // Every call fails the way an unreachable database does, without stopping the shared test database.
    private EntityManagerFactory unavailableDatabase()
    {
        return (EntityManagerFactory) Proxy.newProxyInstance(
                EntityManagerFactory.class.getClassLoader(),
                new Class<?>[] { EntityManagerFactory.class },
                (proxy, method, args) ->
                {
                    throw new IllegalStateException("Database unavailable");
                });
    }
}
