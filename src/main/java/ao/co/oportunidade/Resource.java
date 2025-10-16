package ao.co.oportunidade;

import jakarta.inject.Inject;
import lombok.Getter;

import java.util.Collection;

@Getter
public abstract class Resource <D extends Domain,DS extends DomainService<D,?>>{

    @Inject
    private DS domainService;

}
