package app.persistence.daos;

import app.competence.data.CompetenceDAO;
import app.competence.domain.Competence;
import app.config.HibernateTestConfig;
import app.exceptions.DatabaseException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompetenceDAOTest
{
    private final EntityManagerFactory emf = HibernateTestConfig.getEntityManagerFactory();
    private CompetenceDAO competenceDAO;

    @BeforeEach
    void setUp()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE competences RESTART IDENTITY CASCADE").executeUpdate();
            em.getTransaction().commit();
        }
        competenceDAO = new CompetenceDAO(emf);
    }

    @AfterAll
    void tearDown()
    {
        emf.close();
    }

    @Test
    void createSetsDefaultStateAndTimestamps()
    {
        Competence created = competenceDAO.create(new Competence("Carpenter", BigDecimal.valueOf(850)));

        Competence fetched = competenceDAO.get(created.getId());
        assertThat(fetched.getId(), notNullValue());
        assertThat(fetched.isActive(), is(true));
        assertThat(fetched.getCreatedAt(), notNullValue());
        assertThat(fetched.getUpdatedAt(), notNullValue());
    }

    @Test
    void databaseRejectsCaseInsensitiveDuplicateNames()
    {
        competenceDAO.create(new Competence("Carpenter", BigDecimal.valueOf(850)));

        assertThrows(DatabaseException.class,
                () -> competenceDAO.create(new Competence("carpenter", BigDecimal.valueOf(900))));
    }

    @Test
    void setActivePersistsTheNewState()
    {
        Competence created = competenceDAO.create(new Competence("Carpenter", BigDecimal.valueOf(850)));
        var originalUpdatedAt = created.getUpdatedAt();

        competenceDAO.setActive(created.getId(), false);

        Competence fetched = competenceDAO.get(created.getId());
        assertFalse(fetched.isActive());
        assertThat(fetched.getUpdatedAt(), greaterThan(originalUpdatedAt));
    }
}
