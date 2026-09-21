package app.competence.domain;

import app.competence.presentation.dto.CompetenceDTO;
import app.competence.presentation.dto.CompetenceCreateDTO;
import app.competence.presentation.dto.CompetenceUpdateDTO;
import app.competence.data.ICompetenceDAO;
import app.competence.data.CompetenceMapper;
import app.exceptions.BadRequestException;
import app.exceptions.ConflictException;
import app.utils.ValidationUtil;

import java.util.List;

public class CompetenceService implements ICompetenceService
{
    private final ICompetenceDAO competenceDAO;

    public CompetenceService(ICompetenceDAO competenceDAO)
    {
        this.competenceDAO = competenceDAO;
    }

    @Override
    public CompetenceDTO create(CompetenceCreateDTO dto)
    {
        validate(dto);
        validateNameIsUnique(dto.name().trim().toLowerCase(), null);
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
    public CompetenceDTO update(Long id, CompetenceUpdateDTO dto)
    {
        validate(dto);
        ValidationUtil.validateId(id);
        String name = dto.name().trim().toLowerCase();
        validateNameIsUnique(name, id);
        Competence updated = competenceDAO.get(id);
        updated.update(name, dto.rate());
        updated = competenceDAO.update(updated);
        return CompetenceMapper.toDTO(updated);
    }

    @Override
    public void delete(Long id)
    {
        ValidationUtil.validateId(id);
        if (competenceDAO.isInUse(id))
        {
            throw new ConflictException("Competence is used by one or more tasks and must be deactivated instead");
        }
        competenceDAO.delete(id);
    }

    private void validate(CompetenceCreateDTO dto)
    {
        validate(dto == null ? null : dto.name(), dto == null ? null : dto.rate());
    }

    private void validate(CompetenceUpdateDTO dto)
    {
        validate(dto == null ? null : dto.name(), dto == null ? null : dto.rate());
    }

    private void validate(String name, java.math.BigDecimal rate)
    {
        if (name == null)
        {
            throw new BadRequestException("Competence name is required");
        }

        if (rate == null)
        {
            throw new BadRequestException("Rate is required and must be a number");
        }

        ValidationUtil.validateName(name, "Competence name");

        if (rate.signum() <= 0)
        {
            throw new BadRequestException("Rate must be greater than 0");
        }
    }

    private void validateNameIsUnique(String name, Long excludedId)
    {
        if (competenceDAO.existsByName(name, excludedId))
        {
            throw new BadRequestException("Competence name already exists");
        }
    }

    @Override
    public void setActive(Long id, boolean active)
    {
        competenceDAO.setActive(id, active);
    }
}
