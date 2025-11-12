package solutions.envision.resource;

import jakarta.inject.Inject;
import lombok.Getter;
import solutions.envision.model.Domain;
import solutions.envision.service.DomainService;


@Getter
public abstract class Resource <D extends Domain,DS extends DomainService<D,?>>{

    public static final int OK = 200;
    public static final int SERVER_FAILURE = 503;
    public static final String CONTEXT_PATH = "/api/v1";
    @Inject
    DS domainService;

}
