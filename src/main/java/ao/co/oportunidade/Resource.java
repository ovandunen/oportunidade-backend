package ao.co.oportunidade;

import jakarta.inject.Inject;
import lombok.Getter;


@Getter
public abstract class Resource <D extends Domain,DS extends DomainService<D,?>>{

    public static final int OK = 200;
    public static final int SERVER_FAILURE = 503;
    @Inject
    DS domainService;

}
