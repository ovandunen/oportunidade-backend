package solutions.envision.resource;

import jakarta.inject.Inject;
import lombok.Getter;
import solutions.envision.dto.DTO;
import solutions.envision.dto.DtoMapper;
import solutions.envision.model.Domain;
import solutions.envision.service.DomainService;


@Getter
public abstract class Resource <DT extends DTO,D extends Domain>{

    public static final int OK = 200;
    public static final int SERVER_FAILURE = 503;
    public static final String API_VERSION_PATH = "/api/v1";
    @Inject
    DtoMapper<DT,D> mapper;
    @Inject
    DomainService<D,?> service;

}
