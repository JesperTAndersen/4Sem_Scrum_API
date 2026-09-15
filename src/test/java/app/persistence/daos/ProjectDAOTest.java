package app.persistence.daos;

import app.config.HibernateTestConfig;
import app.entities.Project;
import app.enums.ProjectStatus;
import app.persistence.implementations.ProjectDAO;
import app.persistence.testutils.TestPopulator;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectDAOTest
{
    private final EntityManagerFactory emf = HibernateTestConfig.getEntityManagerFactory();
    private ProjectDAO projectDAO;
    private Map<String, Project> seeded;

    @BeforeEach
    void setUp()
    {
        seeded = TestPopulator.populateProjects(emf);
        projectDAO = new ProjectDAO(emf);
    }

    @AfterAll
    void tearDown()
    {
        emf.close();
    }

    @Test
    @DisplayName("Create - should persist a project, generate an ID, and set timestamps")
    void create()
    {
        Project project = project("New project", ProjectStatus.PLANNED);

        Project created = projectDAO.create(project);

        assertThat(created.getId(), notNullValue());
        Project fetched = projectDAO.get(created.getId());
        assertThat(fetched.getTitle(), is("New project"));
        assertThat(fetched.getStatus(), is(ProjectStatus.PLANNED));
        assertThat(fetched.getCreatedAt(), notNullValue());
        assertThat(fetched.getUpdatedAt(), notNullValue());
    }

    @Test
    @DisplayName("Read - should retrieve seeded projects and all projects in ID order")
    void readAndGetAll()
    {
        Project project = seeded.get("project1");

        assertThat(projectDAO.get(project.getId()).getTitle(), is("Website redesign"));

        List<Project> all = projectDAO.getAll();
        assertThat(all, hasSize(3));
        assertThat(all.stream().map(Project::getId).toList(),
                contains(seeded.get("project1").getId(), seeded.get("project2").getId(), seeded.get("project3").getId()));
    }

    @Test
    @DisplayName("Read and delete - should reject invalid IDs and report missing projects")
    void invalidIdsAndMissingProjects()
    {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> projectDAO.get(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> projectDAO.delete(0L)),
                () -> assertThrows(EntityNotFoundException.class, () -> projectDAO.get(999L)),
                () -> assertThrows(EntityNotFoundException.class, () -> projectDAO.delete(999L))
        );
    }

    @Test
    @DisplayName("Update - should persist changed project fields and update the timestamp")
    void update()
    {
        Project project = seeded.get("project1");
        var previousUpdatedAt = project.getUpdatedAt();

        Project changed = Project.builder()
                .id(project.getId())
                .title("Updated website")
                .description("Updated description")
                .startDate(project.getStartDate())
                .deadline(project.getDeadline())
                .status(ProjectStatus.COMPLETED)
                .createdBy(project.getCreatedBy())
                .updatedBy("admin@example.com")
                .createdAt(project.getCreatedAt())
                .updatedAt(previousUpdatedAt)
                .build();

        projectDAO.update(changed);

        Project fetched = projectDAO.get(project.getId());
        assertThat(fetched.getTitle(), is("Updated website"));
        assertThat(fetched.getStatus(), is(ProjectStatus.COMPLETED));
        assertThat(fetched.getUpdatedAt(), greaterThan(previousUpdatedAt));
    }

    @Test
    @DisplayName("Delete - should remove an existing project")
    void delete()
    {
        Long id = seeded.get("project2").getId();

        assertTrue(projectDAO.delete(id));
        assertThrows(EntityNotFoundException.class, () -> projectDAO.get(id));
    }

    @Test
    @DisplayName("Create and update - should reject null entities and entities without IDs")
    void invalidEntities()
    {
        Project withoutId = project("No ID", ProjectStatus.DRAFT);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> projectDAO.create(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> projectDAO.update(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> projectDAO.update(withoutId))
        );
    }

    private Project project(String title, ProjectStatus status)
    {
        return Project.builder()
                .title(title)
                .description("A test project")
                .startDate(LocalDate.of(2026, 1, 1))
                .deadline(LocalDate.of(2026, 12, 31))
                .status(status)
                .createdBy("test@example.com")
                .updatedBy("test@example.com")
                .build();
    }
}
