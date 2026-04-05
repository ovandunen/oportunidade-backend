package ao.co.oportunidade.payment.initiate.dto;

/**
 * Checkout session returned to the client after initiation (or idempotent replay).
 */
public record PaymentInitiateResponse(String merchantTransactionId, String paymentUrl) {
}
