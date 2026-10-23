package app.project.domain;

import app.exceptions.UnauthorizedException;
import app.persistence.testdoubles.InMemoryProjectDAO;
import app.persistence.testdoubles.InMemoryUserDAO;
import app.project.presentation.dto.CreateProjectDTO;
import app.project.presentation.dto.ProjectDTO;
import app.project.presentation.dto.UpdateProjectDTO;
import app.security.domain.Role;
import app.security.presentation.dto.AuthenticatedUser;
import app.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectServiceTest
{
    private static final AuthenticatedUser MANAGER = new AuthenticatedUser(
            1L, "manager@example.com", Role.PROJECT_MANAGER
    );
    private static final Long EXISTING_PROJECT_ID = 10L;

    private InMemoryProjectDAO projectDAO;
    private ProjectService projectService;
    private User managerUser;

    @BeforeEach
    void setUp()
    {
        InMemoryUserDAO userDAO = new InMemoryUserDAO();
        managerUser = userDAO.addUser("Project", "Manager", "manager@example.com", "Password1!");
        projectDAO = new InMemoryProjectDAO();
        projectDAO.seed(project(EXISTING_PROJECT_ID, "Existing project", ProjectStatus.DRAFT));
        projectService = new ProjectService(projectDAO, userDAO, new ScheduleService());
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvFileSource(resources = "/testcases/project/project-service/create-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Create - validates title, description and dates")
    void createValidatesFields(String title, String description, LocalDate startDate, LocalDate deadline,
                               String expected, String reason)
    {
        CreateProjectDTO dto = new CreateProjectDTO(title, description, startDate, deadline);

        if (expected.equals("ACCEPT"))
        {
            ProjectDTO result = projectService.create(MANAGER, dto);
            assertThat(result.title(), is(title));
            assertThat(result.description(), is(description));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> projectService.create(MANAGER, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {7}")
    @CsvFileSource(resources = "/testcases/project/project-service/update-fields.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Update - validates the project and its fields")
    void updateValidatesFields(Long id, String title, String description, LocalDate startDate, LocalDate deadline,
                               ProjectStatus status, String expected, String reason)
    {
        UpdateProjectDTO dto = new UpdateProjectDTO(title, description, startDate, deadline, status);

        if (expected.equals("ACCEPT"))
        {
            projectService.update(MANAGER, id, dto);
            ProjectDTO stored = projectService.get(id);
            assertThat(stored.title(), is(title));
            assertThat(stored.status(), is(status));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> projectService.update(MANAGER, id, dto));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/project/project-service/get-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Get - requires an existing project")
    void getRequiresExistingProject(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            assertThat(projectService.get(id).id(), is(id));
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> projectService.get(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(resources = "/testcases/project/project-service/delete-by-id.csv", numLinesToSkip = 1, nullValues = "NULL")
    @DisplayName("Delete - requires an existing project")
    void deleteRequiresExistingProject(Long id, String expected, String reason)
    {
        if (expected.equals("ACCEPT"))
        {
            projectService.delete(id);
            assertThat(projectDAO.getStored(id), nullValue());
        }
        else
        {
            Exception exception = assertThrows(Exception.class, () -> projectService.delete(id));
            assertThat(exception.getClass().getSimpleName(), is(expected));
        }
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
        assertThat(created.createdBy().firstName(), is(managerUser.getFirstName()));
        assertThat(created.title(), is(request.title()));
        assertThat(created.startDate(), is(request.startDate()));
        assertThat(created.deadline(), is(request.deadline()));
        assertThat(projectDAO.getLastCreated().getStatus(), is(ProjectStatus.DRAFT));
    }

    @Test
    @DisplayName("Update - should save edited project information")
    void updateSavesBasicInformation()
    {
        UpdateProjectDTO request = new UpdateProjectDTO(
                "Updated title",
                "Updated description",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 8, 31),
                ProjectStatus.PLANNED
        );

        ProjectDTO updated = projectService.update(MANAGER, EXISTING_PROJECT_ID, request);

        assertThat(updated.title(), is("Updated title"));
        assertThat(updated.description(), is("Updated description"));
        assertThat(updated.startDate(), is(request.startDate()));
        assertThat(updated.deadline(), is(request.deadline()));
        assertThat(updated.status(), is(ProjectStatus.PLANNED));
        assertThat(projectDAO.getStored(EXISTING_PROJECT_ID).getTitle(), is("Updated title"));
    }

    @Test
    @DisplayName("Get - should keep planning information available for completed projects")
    void completedProjectRetainsPlanningInformation()
    {
        Project completed = project(20L, "Completed migration", ProjectStatus.COMPLETED);
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
                () -> assertThrows(UnauthorizedException.class, () -> projectService.create(null, createRequest)),
                () -> assertThrows(UnauthorizedException.class,
                        () -> projectService.update(null, EXISTING_PROJECT_ID, updateRequest))
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
                .createdBy(managerUser)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .updatedBy(managerUser)
                .updatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }
}
