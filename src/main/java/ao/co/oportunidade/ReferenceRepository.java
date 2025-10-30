package ao.co.oportunidade;


import ao.co.oportunidade.entity.ReferenceEntity;
import ao.co.oportunidade.webhook.entity.ReferenceEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ReferenceRepository extends Repository<Reference,ReferenceEntity, ReferenceEntityMapper>
{

    @Override
    protected Collection<Reference> findDomains() {

        final List<ReferenceEntity> references = getEntityManager().
                createNamedQuery(ReferenceEntity.FIND_ALL, ReferenceEntity.class).
                getResultStream().toList();
        return references.stream().map(reference -> getMapper().mapToDomain(reference)).toList();
    }


    @Override
    public Optional<Reference> findDomainById(Reference domain) {

        final ReferenceEntity reference = getEntityManager().createNamedQuery(ReferenceEntity.EMPLOYEE_FIND_BY_REFERENCE, ReferenceEntity.class).
                setParameter(ReferenceEntity.PRIMARY_KEY, domain.getId()).getSingleResult();
        return Optional.ofNullable(getMapper().mapToDomain(reference));
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

            return Optional.ofNullable(mapper.mapToDomain(results.getFirst()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
