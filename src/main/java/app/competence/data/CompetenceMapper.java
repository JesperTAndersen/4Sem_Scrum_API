package app.competence.data;

import app.competence.presentation.dto.CompetenceDTO;
import app.competence.presentation.dto.CompetenceCreateDTO;
import app.competence.domain.Competence;

public class CompetenceMapper
{
    public static CompetenceDTO toDTO(Competence competence)
    {
        if (competence == null)
        {
            return null;
        }

        return new CompetenceDTO(
                competence.getId(),
                competence.getName(),
                competence.getRate(),
                competence.isActive(),
                competence.getCreatedAt(),
                competence.getUpdatedAt()
        );
    }

    public static Competence toEntity(CompetenceCreateDTO dto)
    {
        return new Competence(dto.name().trim(), dto.rate());
    }
}
