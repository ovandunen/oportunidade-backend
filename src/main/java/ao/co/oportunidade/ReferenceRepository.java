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
    EntityMapper<Reference,ReferenceEntity> mapper;


    @Override
    protected Collection<Reference> findDomains() {

        final List<ReferenceEntity> referencies = getEntityManager().
                createNamedQuery(ReferenceEntity.FIND_ALL, ReferenceEntity.class).
                getResultStream().toList();
        return referencies.stream().map(reference ->mapper.mapToDomain(reference)).toList();
    }


    /**
     * @param domain
     * @return
     */
    @Override
    protected Optional<Reference> findDomainById(Reference domain) {

        final ReferenceEntity reference = getEntityManager().createNamedQuery(ReferenceEntity.EMPLOYEE_FIND_BY_REFERENCE, ReferenceEntity.class).
                setParameter(ReferenceEntity.PRIMARY_KEY, domain.getId()).getSingleResult();
        return Optional.ofNullable(mapper.mapToDomain(reference));
    }


    /**
     * @param reference
     */
    @Override
    protected void createDomain(final Reference reference) {
        getEntityManager().persist(mapper.mapToEntity(reference));
    }

    /**
     * Find reference by reference number.
     *
     * @param referenceNumber the reference number
     * @return Optional containing the reference if found
     */
    public Optional<Reference> findByReferenceNumber(String referenceNumber) {
        try {
            List<ReferenceEntity> results = getEntityManager()
                    .createQuery("SELECT r FROM ReferenceEntity r WHERE r.referenceNumber = :refNum", ReferenceEntity.class)
                    .setParameter("refNum", referenceNumber)
                    .getResultList();

            if (results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(mapper.mapToDomain(results.get(0)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
