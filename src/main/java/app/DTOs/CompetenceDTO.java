package app.DTOs;

import java.math.BigDecimal;

public record CompetenceDTO
        (
                Long id,
                String name,
                BigDecimal rate
        )
{
}
