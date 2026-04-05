package ao.co.oportunidade.payment.initiate;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "billable_resources")
@IdClass(BillableResourceId.class)
public class BillableResource implements Serializable {

    @Id
    @Column(name = "resource_id", nullable = false, length = 255)
    public String resourceId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 64)
    public PaymentResourceType resourceType;

    @Column(name = "single_buyer", nullable = false)
    public boolean singleBuyer;
}
