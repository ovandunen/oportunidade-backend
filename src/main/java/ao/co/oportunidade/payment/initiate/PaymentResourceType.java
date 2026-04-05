package ao.co.oportunidade.payment.initiate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Billable resource categories for payment initiation.
 */
public enum PaymentResourceType {
    JOB_DOCUMENT,
    JOB_OFFER;

    @JsonCreator
    public static PaymentResourceType fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return PaymentResourceType.valueOf(value.trim());
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
