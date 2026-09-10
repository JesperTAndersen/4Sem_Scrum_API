package app.services.interfaces;

import app.DTOs.CompetenceDTO;

import java.util.List;

public interface CompetenceService
{
    CompetenceDTO create(CompetenceDTO dto);

    CompetenceDTO get(Long id);

    List<CompetenceDTO> getAll();

    CompetenceDTO update(CompetenceDTO dto);

    void delete(Long id);
}
