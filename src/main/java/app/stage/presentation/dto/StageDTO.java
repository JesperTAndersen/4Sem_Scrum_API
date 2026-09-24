package app.stage.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

import app.task.presentation.dto.TaskDTO;

public record StageDTO(
        Long id,
        String name,
        double totalEstimatedHours,
        BigDecimal totalCost,
        List<TaskDTO> tasks)
{
}