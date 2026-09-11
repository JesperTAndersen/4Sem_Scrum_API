package app.mappers;

import app.DTOs.CompetenceDTO;
import app.entities.Competence;

public class CompetenceMapper
{
    public static CompetenceDTO toDTO(Competence competence)
    {
        return new CompetenceDTO(
                competence.getId(),
                competence.getName(),
                competence.getRate()
        );
    }

    public static Competence toEntity(CompetenceDTO dto)
    {
        return new Competence(
                dto.id(),
                dto.name(),
                dto.rate()
        );
    }
}
