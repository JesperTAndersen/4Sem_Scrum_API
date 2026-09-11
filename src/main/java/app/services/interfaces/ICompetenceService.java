package app.services.interfaces;

import app.dtos.CompetenceDTO;

import java.util.List;

public interface ICompetenceService
{
    CompetenceDTO create(CompetenceDTO dto);

    CompetenceDTO get(Long id);

    List<CompetenceDTO> getAll();

    CompetenceDTO update(CompetenceDTO dto);

    void delete(Long id);
}
