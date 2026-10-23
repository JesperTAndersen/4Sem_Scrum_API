package app.persistence.testdoubles;

import app.exceptions.NotFoundException;
import app.stage.data.IStageDAO;
import app.stage.domain.Stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mirrors {@code StageDAO}: a missing stage raises {@link NotFoundException}.
 */
public class InMemoryStageDAO implements IStageDAO
{
    private final Map<Long, Stage> stages = new LinkedHashMap<>();
    private long nextId = 1L;

    @Override
    public Stage create(Stage stage)
    {
        EntityIds.assign(stage, nextId++);
        stages.put(stage.getId(), stage);
        return stage;
    }

    @Override
    public Stage get(Long id)
    {
        Stage stage = stages.get(id);
        if (stage == null)
        {
            throw new NotFoundException("No stage found with id: " + id);
        }
        return stage;
    }

    @Override
    public List<Stage> getAll()
    {
        return new ArrayList<>(stages.values());
    }

    @Override
    public Stage update(Stage stage)
    {
        get(stage.getId());
        stages.put(stage.getId(), stage);
        return stage;
    }

    @Override
    public boolean delete(Long id)
    {
        get(id);
        return stages.remove(id) != null;
    }

    public Stage getStored(Long id)
    {
        return stages.get(id);
    }
}
