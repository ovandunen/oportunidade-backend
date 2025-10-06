package ao.co.oportunidade.webhook.test;

import ao.co.oportunidade.webhook.dto.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Builder for creating test webhook payloads.
 * Provides convenient methods to create realistic test data.
 */
public class WebhookPayloadBuilder {

    /**
     * Create a success payment payload with default values.
     */
    public static AppyPayWebhookPayload createSuccessPayload() {
        return AppyPayWebhookPayload.builder()
                .id("tx-success-" + System.currentTimeMillis())
                .merchantTransactionId("ORDER-" + System.currentTimeMillis())
                .type("Charge")
                .amount(new BigDecimal("1500.00"))
                .currency("AOA")
                .status("Success")
                .paymentMethod("REF")
                .reference(createReferenceInfo())
                .customer(createCustomerInfo())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }

    /**
     * Create a pending payment payload with default values.
     */
    public static AppyPayWebhookPayload createPendingPayload() {
        return AppyPayWebhookPayload.builder()
                .id("tx-pending-" + System.currentTimeMillis())
                .merchantTransactionId("ORDER-" + System.currentTimeMillis())
                .type("Charge")
                .amount(new BigDecimal("2000.00"))
                .currency("AOA")
                .status("Pending")
                .paymentMethod("REF")
                .reference(createReferenceInfo())
                .customer(createCustomerInfo())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }

    /**
     * Create a failed payment payload with default values.
     */
    public static AppyPayWebhookPayload createFailedPayload() {
        return AppyPayWebhookPayload.builder()
                .id("tx-failed-" + System.currentTimeMillis())
                .merchantTransactionId("ORDER-" + System.currentTimeMillis())
                .type("Charge")
                .amount(new BigDecimal("3000.00"))
                .currency("AOA")
                .status("Failed")
                .paymentMethod("REF")
                .reference(createReferenceInfo())
                .customer(createCustomerInfo())
                .responseStatus(ResponseStatus.builder()
                        .code("ERROR")
                        .message("Payment failed")
                        .success(false)
                        .errorCode("INSUFFICIENT_FUNDS")
                        .errorDetails("Insufficient funds in account")
                        .build())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }

    /**
     * Create a cancelled payment payload with default values.
     */
    public static AppyPayWebhookPayload createCancelledPayload() {
        return AppyPayWebhookPayload.builder()
                .id("tx-cancelled-" + System.currentTimeMillis())
                .merchantTransactionId("ORDER-" + System.currentTimeMillis())
                .type("Charge")
                .amount(new BigDecimal("2500.00"))
                .currency("AOA")
                .status("Cancelled")
                .paymentMethod("REF")
                .reference(createReferenceInfo())
                .customer(createCustomerInfo())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }

    /**
     * Create a custom payload with specified values.
     */
    public static AppyPayWebhookPayload createCustomPayload(
            String transactionId,
            String merchantTxId,
            BigDecimal amount,
            String status) {
        return AppyPayWebhookPayload.builder()
                .id(transactionId)
                .merchantTransactionId(merchantTxId)
                .type("Charge")
                .amount(amount)
                .currency("AOA")
                .status(status)
                .paymentMethod("REF")
                .reference(createReferenceInfo())
                .customer(createCustomerInfo())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }

    /**
     * Create default reference info.
     */
    public static ReferenceInfo createReferenceInfo() {
        return ReferenceInfo.builder()
                .referenceNumber("123456789")
                .entity("00123")
                .dueDate(Instant.now().plusSeconds(86400)) // 24 hours
                .startDate(Instant.now())
                .status("Active")
                .build();
    }

    /**
     * Create default customer info.
     */
    public static CustomerInfo createCustomerInfo() {
        return CustomerInfo.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+244900000000")
                .documentNumber("123456789BA")
                .documentType("ID")
                .build();
    }

    /**
     * Create custom reference info.
     */
    public static ReferenceInfo createReferenceInfo(String referenceNumber, String entity) {
        return ReferenceInfo.builder()
                .referenceNumber(referenceNumber)
                .entity(entity)
                .dueDate(Instant.now().plusSeconds(86400))
                .startDate(Instant.now())
                .status("Active")
                .build();
    }

    /**
     * Create custom customer info.
     */
    public static CustomerInfo createCustomerInfo(String name, String email, String phone) {
        return CustomerInfo.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .documentNumber("DOC123")
                .documentType("ID")
                .build();
    }
}
