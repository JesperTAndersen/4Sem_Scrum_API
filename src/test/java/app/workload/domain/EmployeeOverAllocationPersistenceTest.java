package app.workload.domain;

import app.capacity.domain.CompanyCapacity;
import app.competence.domain.Competence;
import app.config.HibernateTestConfig;
import app.employee.domain.Employee;
import app.persistence.testutils.TestPopulator;
import app.task.data.TaskDAO;
import app.project.domain.Project;
import app.stage.domain.Stage;
import app.task.domain.Task;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeOverAllocationPersistenceTest
{
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private final EntityManagerFactory emf = HibernateTestConfig.getEntityManagerFactory();

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(resources = "/testcases/planning/employee-over-allocation-service/calculate-persisted-changes.csv",
            numLinesToSkip = 1)
    @DisplayName("Find conflicts - loads detached project graphs and observes persisted planning changes")
    void calculatesAfterReload(String change, double allocatedHours, double excessHours,
                               int unassessedCount, String reason)
    {
        List<Project> projects = TestPopulator.populateProjects(emf).values().stream().limit(2).toList();
        Long firstId;
        Long secondId;
        Long workId;
        Long companyId;
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE employee, competences RESTART IDENTITY CASCADE").executeUpdate();
            Project firstProject = em.find(Project.class, projects.getFirst().getId());
            Project secondProject = em.find(Project.class, projects.get(1).getId());
            firstProject = withStart(em, firstProject, MONDAY);
            secondProject = withStart(em, secondProject, MONDAY.plusDays(1));
            CompanyCapacity company = em.find(CompanyCapacity.class, CompanyCapacity.STANDARD_ID);
            if (company == null)
            {
                company = new CompanyCapacity();
                em.persist(company);
            }
            company.update(7.5);
            Competence competence = new Competence("Planning " + firstProject.getId(), BigDecimal.TEN);
            em.persist(competence);
            Employee first = new Employee("First", "Worker", 7.5);
            first.configureCapacity(true, company);
            first.addCompetence(competence);
            Employee second = new Employee("Second", "Worker", 2.5);
            second.addCompetence(competence);
            em.persist(first);
            em.persist(second);
            Stage precedingStage = new Stage("Predecessor stage", firstProject);
            em.persist(precedingStage);
            Task predecessor = new Task(precedingStage, "Completed predecessor", 1);
            predecessor.setCompetence(null, 7.5);
            predecessor.changeStatus(Task.TaskStatus.DONE);
            predecessor.recordPlanningSnapshot(MONDAY, MONDAY, 1.0, 1.0);
            em.persist(predecessor);
            Stage stage = new Stage("Work stage", firstProject);
            stage.getPredecessors().add(precedingStage);
            em.persist(stage);
            Task work = new Task(stage, "Planned work", 0);
            work.setCompetence(competence, 30);
            work.replaceEmployees(List.of(first, second));
            em.persist(work);
            Stage otherStage = new Stage("Other project", secondProject);
            em.persist(otherStage);
            Task otherWork = new Task(otherStage, "Other work", 1);
            otherWork.setCompetence(null, 2.5);
            otherWork.assignEmployee(first);
            em.persist(otherWork);
            em.getTransaction().commit();
            firstId = first.getId();
            secondId = second.getId();
            workId = work.getId();
            companyId = company.getId();
        }

        EmployeeWorkloadService service = new EmployeeWorkloadService(new TaskDAO(emf));
        var baseline = service.findConflicts(MONDAY.plusDays(1), MONDAY.plusDays(1));
        assertEquals(1, baseline.conflicts().size());
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            switch (change)
            {
                case "NONE" -> { }
                case "STANDARD_CAPACITY" -> em.find(CompanyCapacity.class, companyId).update(2.5);
                case "INACTIVE" -> em.find(Employee.class, firstId).setActive(false);
                case "COMPETENCE_REMOVED" -> em.find(Employee.class, firstId).replaceCompetences(List.of());
                case "ASSIGNMENT_REMOVED" ->
                {
                    Task work = em.find(Task.class, workId);
                    work.unassignEmployee(em.find(Employee.class, secondId));
                }
                case "DONE" ->
                {
                    Task work = em.find(Task.class, workId);
                    work.recordPlanningSnapshot(MONDAY.plusDays(1), MONDAY.plusDays(3), 3.0, 3.0);
                    work.changeStatus(Task.TaskStatus.DONE);
                }
                case "DATES_MOVED" -> withStart(em,
                        em.find(Project.class, projects.get(1).getId()), MONDAY.plusDays(2));
                default -> throw new AssertionError("Unknown change: " + change);
            }
            em.getTransaction().commit();
        }
        var result = service.findConflicts(MONDAY.plusDays(1), MONDAY.plusDays(1));
        assertEquals(unassessedCount, result.unassessedTaskIds().size());
        assertEquals(excessHours > 0 ? 1 : 0, result.conflicts().size());
        if (excessHours > 0)
        {
            var conflict = result.conflicts().getFirst();
            assertEquals(firstId, conflict.employeeId());
            assertEquals(allocatedHours, conflict.allocatedHours(), 0.000000001);
            assertEquals(excessHours, conflict.excessHours(), 0.000000001);
            assertEquals(2, conflict.taskHours().size());
        }
    }

    private Project withStart(EntityManager em, Project project, LocalDate start)
    {
        return em.merge(Project.builder().id(project.getId()).title(project.getTitle())
                .description(project.getDescription()).status(project.getStatus())
                .createdBy(project.getCreatedBy()).updatedBy(project.getUpdatedBy())
                .createdAt(project.getCreatedAt()).updatedAt(project.getUpdatedAt())
                .startDate(start).deadline(start.plusDays(1)).build());
    }
}
