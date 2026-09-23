package app.competence.data;

import app.competence.domain.Competence;
import app.shared.data.ICrudDAO;

public interface ICompetenceDAO extends ICrudDAO<Competence>
{
    boolean existsByName(String name, Long excludedId);

    boolean isInUse(Long id);

    void setActive(Long id, boolean active);
}
