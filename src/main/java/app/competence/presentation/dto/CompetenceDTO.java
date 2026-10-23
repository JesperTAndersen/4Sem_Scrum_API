package app.competence.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetenceDTO
        (
                Long id,
                String name,
                BigDecimal rate,
                Boolean active,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        )
{
}
