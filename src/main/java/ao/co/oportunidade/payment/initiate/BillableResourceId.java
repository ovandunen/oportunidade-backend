package ao.co.oportunidade.payment.initiate;

import java.io.Serializable;
import java.util.Objects;

public class BillableResourceId implements Serializable {

    public String resourceId;
    public PaymentResourceType resourceType;

    public BillableResourceId() {
    }

    public BillableResourceId(String resourceId, PaymentResourceType resourceType) {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BillableResourceId that = (BillableResourceId) o;
        return Objects.equals(resourceId, that.resourceId) && resourceType == that.resourceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, resourceType);
    }
}
