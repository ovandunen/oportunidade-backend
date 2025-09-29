package ao.co.oportunidade;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.Collection;
import java.util.Optional;

public abstract class Repository<D extends Domain,DE extends DomainEntity> implements PanacheRepository<DE> {

   protected abstract Collection<D> findDomains();
   protected abstract Optional<D> findDomainById(D domain);
   protected abstract void createDomain (D domain);

}
