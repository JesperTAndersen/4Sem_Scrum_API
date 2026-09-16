package app.competence.domain;

import app.competence.presentation.dto.CompetenceDTO;
import app.competence.data.ICompetenceDAO;
import app.competence.domain.Competence;
import app.exceptions.ApiException;
import app.competence.data.CompetenceMapper;
import app.competence.domain.ICompetenceService;

import java.util.List;

public class CompetenceService implements ICompetenceService
{
    private final ICompetenceDAO competenceDAO;

    public CompetenceService(ICompetenceDAO competenceDAO)
    {
        this.competenceDAO = competenceDAO;
    }

    @Override
    public CompetenceDTO create(CompetenceDTO dto)
    {
        validate(dto);
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
        validate(dto);
        Competence updated = competenceDAO.update(CompetenceMapper.toEntity(dto));
        return CompetenceMapper.toDTO(updated);
    }

    @Override
    public void delete(Long id)
    {
        competenceDAO.delete(id);
    }

    private void validate(CompetenceDTO dto)
    {
        if (dto == null || dto.name() == null || dto.name().isBlank())
        {
            throw new ApiException(400, "Name is required");
        }

        if (dto.rate() == null)
        {
            throw new ApiException(400, "Rate is required and must be a number");
        }
    }
}
