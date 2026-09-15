package app.persistence.testdoubles;

import app.entities.Project;
import app.persistence.interfaces.specific.IProjectDAO;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryProjectDAO implements IProjectDAO
{
    private final Map<Long, Project> projects = new LinkedHashMap<>();
    private long nextId = 1L;
    private Project lastCreated;

    @Override
    public Project create(Project project)
    {
        Project persisted = copyWithId(project, nextId++);
        projects.put(persisted.getId(), persisted);
        lastCreated = persisted;
        return persisted;
    }

    @Override
    public Project get(Long id)
    {
        Project project = projects.get(id);
        if (project == null)
        {
            throw new EntityNotFoundException("Project not found");
        }
        return project;
    }

    @Override
    public List<Project> getAll()
    {
        return new ArrayList<>(projects.values());
    }

    @Override
    public Project update(Project project)
    {
        get(project.getId());
        projects.put(project.getId(), project);
        return project;
    }

    @Override
    public boolean delete(Long id)
    {
        return projects.remove(id) != null;
    }

    public void seed(Project project)
    {
        projects.put(project.getId(), project);
        nextId = Math.max(nextId, project.getId() + 1);
    }

    public Project getLastCreated()
    {
        return lastCreated;
    }

    public Project getStored(Long id)
    {
        return projects.get(id);
    }

    private Project copyWithId(Project project, Long id)
    {
        LocalDateTime now = LocalDateTime.now();
        return Project.builder()
                .id(id)
                .title(project.getTitle())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .deadline(project.getDeadline())
                .status(project.getStatus())
                .createdBy(project.getCreatedBy())
                .createdAt(now)
                .updatedBy(project.getUpdatedBy())
                .updatedAt(now)
                .build();
    }
}
