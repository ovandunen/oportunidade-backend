package ao.co.oportunidade;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.util.UUID;

public abstract class DomainEntity extends PanacheEntityBase {

    public abstract UUID getId();

}
