package app.services;

import app.DTOs.CompetenceDTO;
import app.dao.CompetenceDAO;
import app.entities.Competence;
import app.mappers.CompetenceMapper;
import app.services.interfaces.CompetenceService;

import java.util.List;

public class CompetenceServiceImpl implements CompetenceService
{
    private final CompetenceDAO competenceDAO;

    public CompetenceServiceImpl(CompetenceDAO competenceDAO)
    {
        this.competenceDAO = competenceDAO;
    }

    @Override
    public CompetenceDTO create(CompetenceDTO dto)
    {
        Competence created = competenceDAO.create(CompetenceMapper.toEntity(dto));
        return CompetenceMapper.toDTO(created);
    }

    @Override
    public CompetenceDTO get(Long id)
    {
        return CompetenceMapper.toDTO(competenceDAO.get(id));
    }

    @Override
    public List<CompetenceDTO> getAll()
    {
        return competenceDAO.getAll().stream()
                .map(CompetenceMapper::toDTO)
                .toList();
    }

    @Override
    public CompetenceDTO update(CompetenceDTO dto)
    {
        Competence updated = competenceDAO.update(CompetenceMapper.toEntity(dto));
        return CompetenceMapper.toDTO(updated);
    }

    @Override
    public void delete(Long id)
    {
        competenceDAO.delete(competenceDAO.get(id));
    }
}
