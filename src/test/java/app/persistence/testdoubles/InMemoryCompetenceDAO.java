package app.persistence.testdoubles;

import app.competence.data.ICompetenceDAO;
import app.competence.domain.Competence;
import app.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Mirrors {@code CompetenceDAO}: a missing competence raises {@link NotFoundException}, and name lookups ignore
 * case and surrounding whitespace like the database query.
 */
public class InMemoryCompetenceDAO implements ICompetenceDAO
{
    private final Map<Long, Competence> competences = new LinkedHashMap<>();
    private final Set<Long> usedByTasks = new HashSet<>();
    private long nextId = 1L;

    @Override
    public Competence create(Competence competence)
    {
        EntityIds.assign(competence, nextId++);
        competences.put(competence.getId(), competence);
        return competence;
    }

    @Override
    public Competence get(Long id)
    {
        Competence competence = competences.get(id);
        if (competence == null)
        {
            throw new NotFoundException("No competence found with id: " + id);
        }
        return competence;
    }

    @Override
    public List<Competence> getAll()
    {
        return new ArrayList<>(competences.values());
    }

    @Override
    public Competence update(Competence competence)
    {
        get(competence.getId());
        competences.put(competence.getId(), competence);
        return competence;
    }

    @Override
    public boolean delete(Long id)
    {
        get(id);
        return competences.remove(id) != null;
    }

    @Override
    public boolean existsByName(String name, Long excludedId)
    {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return competences.values().stream()
                .filter(competence -> !Objects.equals(competence.getId(), excludedId))
                .anyMatch(competence -> competence.getName().toLowerCase(Locale.ROOT).equals(normalized));
    }

    @Override
    public boolean isInUse(Long id)
    {
        return usedByTasks.contains(id);
    }

    @Override
    public void setActive(Long id, boolean active)
    {
        get(id).setActive(active);
    }

    public void markAsUsedByTask(Long id)
    {
        usedByTasks.add(id);
    }
}
