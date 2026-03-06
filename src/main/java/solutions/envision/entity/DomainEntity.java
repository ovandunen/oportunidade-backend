package solutions.envision.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.util.UUID;

public abstract class DomainEntity extends PanacheEntityBase {

    public static final String PRIMARY_KEY = "id";
    public abstract UUID getId();

}
