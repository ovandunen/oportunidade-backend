package ao.co.oportunidade.payment.initiate.dto;

import ao.co.oportunidade.payment.initiate.PaymentResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code POST /api/v1/payments/initiate}. User identity comes from the JWT only.
 */
public record PaymentInitiateRequest(
        @NotBlank(message = "resourceId is required") String resourceId,
        @NotNull(message = "resourceType is required") PaymentResourceType resourceType) {
}
