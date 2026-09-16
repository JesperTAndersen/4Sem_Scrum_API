package app.competence.presentation.dto;

import app.utils.StrictBigDecimalDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

public record CompetenceDTO
        (
                Long id,
                String name,
                @JsonDeserialize(using = StrictBigDecimalDeserializer.class)
                BigDecimal rate
        )
{
}
