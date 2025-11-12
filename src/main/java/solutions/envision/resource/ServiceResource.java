package solutions.envision.resource;

import jakarta.inject.Inject;
import lombok.Getter;
import solutions.envision.dto.DTO;
import solutions.envision.model.Domain;
import solutions.envision.service.DomainService;

@Getter
public abstract class ServiceResource<DT extends DTO, D extends Domain, DS extends DomainService<D,?>>
        extends Resource<DT,D> {

    @Inject
    DS domainService;
}
