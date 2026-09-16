package app.shared.presentation;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import app.shared.presentation.ICrudController;
import app.shared.domain.ICrudService;
import app.utils.RequestUtil;
import io.javalin.http.Context;

public abstract class CrudController<createDTO, DTO> implements ICrudController
{
    private ICrudService<createDTO, DTO> service;

    private Class<createDTO> createDTOClass;
    private Class<DTO> DTOClass;

    @SuppressWarnings("unchecked")
    protected CrudController(ICrudService<createDTO, DTO> service)
    {
        this.service = service;
        Type[] types = ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments();
        this.createDTOClass = (Class<createDTO>)types[0];
        this.DTOClass = (Class<DTO>)types[1];
    }

    @Override
    public void get(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        DTO result;
        result = service.get(id);
        ctx.status(200).json(result);
    }

    @Override
    public void getAll(Context ctx)
    {
        List<DTO> result;
        result = service.getAll();
        ctx.status(200).json(result);
    }

    @Override
    public void create(Context ctx)
    {
        createDTO createDTO;
        DTO dto;
        createDTO = ctx.bodyAsClass(createDTOClass);
        dto = service.create(createDTO);
        ctx.status(201).json(dto);
    }


    @Override
    public void update(Context ctx)
    {
        DTO dto = ctx.bodyAsClass(DTOClass);
        dto = service.update(dto);
        ctx.status(200).json(dto);
    }

    @Override
    public void delete(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");
        service.delete(id);
        ctx.status(200);
    }
}
