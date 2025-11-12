package solutions.envision.entity;

import solutions.envision.model.Domain;

public interface EntityMapper<D extends Domain, E extends DomainEntity> {

    E mapToEntity(D domain);
    D mapToDomain(E entity);
}
