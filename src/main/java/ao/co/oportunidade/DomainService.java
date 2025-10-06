package ao.co.oportunidade;

import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


@Getter(value = AccessLevel.PROTECTED)
public abstract class DomainService <D extends Domain, R extends Repository<D,?>> {

    @Inject
    R  repository;

    protected abstract Collection<D> getAllDomains();
    protected abstract void createDomain(D  domain);

    protected void validateDomain(D domain) throws DomainNotCreatedException {
        UUID id = Optional.ofNullable(domain).orElseThrow(() -> new NullPointerException(
                "Domain  does not exist"
        )).getId();
        Optional.ofNullable(id).orElseThrow(() -> new DomainNotCreatedException("Domain id is null"));
    }
}
