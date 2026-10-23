package app.competence.domain;

import app.competence.presentation.dto.CompetenceCreateDTO;
import app.competence.presentation.dto.CompetenceDTO;
import app.competence.presentation.dto.CompetenceUpdateDTO;

import java.util.List;

public interface ICompetenceService
{
    CompetenceDTO create(CompetenceCreateDTO dto);

    CompetenceDTO get(Long id);

    List<CompetenceDTO> getAll();

    CompetenceDTO update(Long id, CompetenceUpdateDTO dto);

    void delete(Long id);

    void setActive(Long id, boolean active);
}
