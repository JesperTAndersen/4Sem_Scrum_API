package app.services;

import app.project.presentation.dto.CreateProjectDTO;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.UpdateProjectDTO;
import app.security.presentation.dto.AuthenticatedUser;
import app.project.domain.Project;
import app.project.domain.ProjectStatus;
import app.user.domain.User;
import app.security.domain.Role;
import app.exceptions.ApiException;
import app.persistence.testdoubles.InMemoryProjectDAO;
import app.persistence.testdoubles.InMemoryUserDAO;
import app.project.domain.ProjectService;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest
{
    private static final AuthenticatedUser MANAGER = new AuthenticatedUser(
            1L, "manager@example.com", Role.PROJECT_MANAGER
    );
    private static final User MANAGER_USER = new User(
            "Project", "Manager", "manager@example.com", "hashed-password"
    );

    private InMemoryProjectDAO projectDAO;
    private ProjectService projectService;

    @BeforeEach
    void setUp()
    {
        projectDAO = new InMemoryProjectDAO();
        projectService = new ProjectService(projectDAO, new InMemoryUserDAO(MANAGER_USER));
    }

    @Test
    @DisplayName("Create - should create a project with DRAFT status and the authenticated user as owner")
    void createStartsAsDraft()
    {
        CreateProjectDTO request = new CreateProjectDTO(
                "Website redesign",
                "Redesign the public website",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30)
        );

        ProjectDTO created = projectService.create(MANAGER, request);

        assertThat(created.status(), is(ProjectStatus.DRAFT));
        assertThat(created.createdBy().firstName(), is(MANAGER_USER.getFirstName()));
        assertThat(created.title(), is(request.title()));
        assertThat(created.startDate(), is(request.startDate()));
        assertThat(created.deadline(), is(request.deadline()));
        assertThat(projectDAO.getLastCreated().getStatus(), is(ProjectStatus.DRAFT));
    }

    @Test
    @DisplayName("Create - should reject a deadline before the project start date")
    void createRejectsInvalidDateRange()
    {
        CreateProjectDTO request = new CreateProjectDTO(
                "Invalid project",
                "The dates are in the wrong order",
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 1, 1)
        );

        ApiException exception = assertThrows(ApiException.class,
                () -> projectService.create(MANAGER, request));

        assertThat(exception.getCode(), is(400));
        assertThat(exception.getMessage(), is("Project deadline cannot be before start date"));
        assertThat(projectDAO.getLastCreated(), nullValue());
    }

    @Test
    @DisplayName("Update - should save edited project information")
    void updateSavesBasicInformation()
    {
        Project existing = project(10L, "Old title", ProjectStatus.DRAFT);
        projectDAO.seed(existing);

        UpdateProjectDTO request = new UpdateProjectDTO(
                "Updated title",
                "Updated description",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 8, 31),
                ProjectStatus.PLANNED
        );

        ProjectDTO updated = projectService.update(MANAGER, existing.getId(), request);

        assertThat(updated.title(), is("Updated title"));
        assertThat(updated.description(), is("Updated description"));
        assertThat(updated.startDate(), is(request.startDate()));
        assertThat(updated.deadline(), is(request.deadline()));
        assertThat(updated.status(), is(ProjectStatus.PLANNED));
        assertThat(projectDAO.getStored(existing.getId()).getTitle(), is("Updated title"));
    }

    @Test
    @DisplayName("Update - should persist a changed project status")
    void updateSavesStatus()
    {
        Project existing = project(10L, "Project", ProjectStatus.DRAFT);
        projectDAO.seed(existing);

        UpdateProjectDTO request = new UpdateProjectDTO(
                existing.getTitle(), existing.getDescription(), existing.getStartDate(),
                existing.getDeadline(), ProjectStatus.IN_PROGRESS
        );

        projectService.update(MANAGER, existing.getId(), request);

        assertThat(projectService.get(existing.getId()).status(), is(ProjectStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("Get - should keep planning information available for completed projects")
    void completedProjectRetainsPlanningInformation()
    {
        Project completed = project(10L, "Completed migration", ProjectStatus.COMPLETED);
        projectDAO.seed(completed);

        ProjectDTO result = projectService.get(completed.getId());

        assertThat(result.status(), is(ProjectStatus.COMPLETED));
        assertThat(result.title(), is(completed.getTitle()));
        assertThat(result.description(), is(completed.getDescription()));
        assertThat(result.startDate(), is(completed.getStartDate()));
        assertThat(result.deadline(), is(completed.getDeadline()));
    }

    @Test
    @DisplayName("Create and update - should require an authenticated user")
    void protectedOperationsRequireAuthentication()
    {
        CreateProjectDTO createRequest = new CreateProjectDTO(
                "Project", "Description", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)
        );
        UpdateProjectDTO updateRequest = new UpdateProjectDTO(
                "Project", "Description", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), ProjectStatus.DRAFT
        );

        assertAll(
                () -> assertThrows(ApiException.class, () -> projectService.create(null, createRequest)),
                () -> assertThrows(ApiException.class, () -> projectService.update(null, 1L, updateRequest))
        );
    }

    private Project project(Long id, String title, ProjectStatus status)
    {
        return Project.builder()
                .id(id)
                .title(title)
                .description("Project planning information")
                .startDate(LocalDate.of(2026, 1, 1))
                .deadline(LocalDate.of(2026, 12, 31))
                .status(status)
                .createdBy(MANAGER_USER)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedBy(MANAGER_USER)
                .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }

}
