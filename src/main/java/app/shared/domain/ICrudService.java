package app.shared.domain;

import java.util.List;

public interface ICrudService<CreateDTO, DTO>
{
    DTO create(CreateDTO dto);

    DTO get(Long id);

    List<DTO> getAll();

    DTO update(DTO dto);

    void delete(Long id);
}
