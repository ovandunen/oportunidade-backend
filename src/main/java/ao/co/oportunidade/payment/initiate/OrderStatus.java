package ao.co.oportunidade.payment.initiate;

/**
 * Lifecycle for {@link PaymentInitiationOrder} (paywall checkout).
 */
public enum OrderStatus {
    PENDING,
    PAID,
    FAILED,
    EXPIRED
}
