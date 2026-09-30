package app.stage.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import app.task.presentation.dto.SlimTaskDTO;

public record StageDTO(
        Long id,
        String name,
        double totalEstimatedHours,
        BigDecimal totalCost,
        LocalDate startDate,
        LocalDate endDate,
        List<SlimTaskDTO> tasks)
{
}
