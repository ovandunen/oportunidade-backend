package ao.co.oportunidade.entity;

import ao.co.oportunidade.Domain;
import ao.co.oportunidade.DomainEntity;

public interface EntityMapper<D extends Domain, E extends DomainEntity> {

    E mapToEntity(D domain);
    D mapToDomain(E entity);
}
