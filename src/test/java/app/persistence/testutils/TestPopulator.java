package app.persistence.testutils;

import app.entities.Project;
import app.entities.User;
import app.enums.ProjectStatus;
import app.enums.Role;
import app.utils.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestPopulator
{
    private TestPopulator()
    {
    }

    public static Map<String, Project> populateProjects(EntityManagerFactory emf)
    {
        Project project1 = project("Website redesign", "Redesign the public website", ProjectStatus.IN_PROGRESS,
                "manager@example.com", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 6, 30));
        Project project2 = project("Mobile application", "Build the mobile application", ProjectStatus.DRAFT,
                "manager@example.com", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 10, 31));
        Project project3 = project("Completed migration", "Migrate the legacy platform", ProjectStatus.COMPLETED,
                "admin@example.com", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE projects RESTART IDENTITY CASCADE").executeUpdate();
            em.persist(project1);
            em.persist(project2);
            em.persist(project3);
            em.getTransaction().commit();
        }

        Map<String, Project> seeded = new LinkedHashMap<>();
        seeded.put("project1", project1);
        seeded.put("project2", project2);
        seeded.put("project3", project3);
        return seeded;
    }

    public static Map<String, User> populateUsers(EntityManagerFactory emf)
    {
        String password = PasswordUtil.hashPassword("Password123", 4);
        User user1 = new User("Alice", "Manager", "alice@example.com", password);
        user1.changeRole(Role.PROJECT_MANAGER);
        User user2 = new User("Bob", "Employee", "bob@example.com", password);
        User user3 = new User("Carol", "Employee", "carol@example.com", password);

        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate();
            em.persist(user1);
            em.persist(user2);
            em.persist(user3);
            em.getTransaction().commit();
        }

        Map<String, User> seeded = new LinkedHashMap<>();
        seeded.put("user1", user1);
        seeded.put("user2", user2);
        seeded.put("user3", user3);
        return seeded;
    }

    private static Project project(String title, String description, ProjectStatus status, String user,
                                   LocalDate startDate, LocalDate deadline)
    {
        return Project.builder()
                .title(title)
                .description(description)
                .startDate(startDate)
                .deadline(deadline)
                .status(status)
                .createdBy(user)
                .updatedBy(user)
                .build();
    }
}
