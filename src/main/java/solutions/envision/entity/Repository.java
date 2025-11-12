package solutions.envision.entity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.inject.Inject;
import lombok.Getter;
import solutions.envision.model.Domain;

import java.util.Collection;
import java.util.Optional;

@Getter
public abstract class Repository<D extends Domain,DE extends DomainEntity, M extends EntityMapper<D,DE>> implements PanacheRepository<DE> {


    @Inject
    M mapper;

   protected abstract Collection<D> findDomains();
   public abstract Optional<D> findDomainById(D domain);
   public final void save(D domain) {

       final DE domainEntity = mapper.mapToEntity(domain);
       final Optional<D> domainById = findDomainById(domain);
       if(domainById.isPresent()) {
           getEntityManager().merge(domainEntity);
       } else  {
           getEntityManager().persist(domainEntity);
       }
   }

}
