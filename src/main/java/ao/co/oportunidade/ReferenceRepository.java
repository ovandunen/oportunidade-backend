package ao.co.oportunidade;


import ao.co.oportunidade.entity.EntityMapper;
import ao.co.oportunidade.entity.ReferenceEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.SessionFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class ReferenceRepository extends Repository<Reference,ReferenceEntity>
{

    @Inject
    private EntityMapper<Reference,ReferenceEntity> mapper;

    /**
     * @return
     */
    @Override
    protected Collection<Reference> findDomains() {

        final List<ReferenceEntity> referencies = getEntityManager().
                createNamedQuery(ReferenceEntity.FIND_ALL, ReferenceEntity.class).
                getResultStream().toList();
        return referencies.stream().map(reference ->mapper.mapEntityToDomain(reference)).toList();
    }

    /**
     * @param domain
     * @return
     */
    @Override
    protected Optional<Reference> findDomainById(Reference domain) {

        final ReferenceEntity reference = getEntityManager().createNamedQuery(ReferenceEntity.EMPLOYEE_FIND_BY_REFERENCE, ReferenceEntity.class).
                setParameter(ReferenceEntity.PRIMARY_KEY, domain.getId()).getSingleResult();
        return Optional.ofNullable(mapper.mapEntityToDomain(reference));
    }

    /**
     * @param reference
     */
    @Override
    protected void createDomain(Reference reference) {
        getEntityManager().persist(mapper.mapDomainToEntity(reference));
    }
}
