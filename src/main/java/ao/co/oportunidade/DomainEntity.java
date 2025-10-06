package ao.co.oportunidade;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import java.util.UUID;

public abstract class DomainEntity extends PanacheEntity {

    public abstract UUID getId();

}
