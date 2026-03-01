package ao.co.oportunidade.payment.service;

import ao.co.oportunidade.payment.dto.PaymentReferenceRequest;
import ao.co.oportunidade.payment.dto.PaymentReferenceResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Service that issues MultiCaixa payment references as plain text.
 * Compatible with MultiCaixa ATM/agent payment flow in Angola.
 * Does not replace AppyPay – provides an alternative reference issuance channel.
 */
@ApplicationScoped
public class PaymentReferenceTextService {

    private static final Logger LOG = Logger.getLogger(PaymentReferenceTextService.class);
    private static final String DEFAULT_ENTITY = "00123";
    private static final String CURRENCY_AOA = "AOA";
    private static final int REFERENCE_LENGTH = 9;
    private static final int DEFAULT_VALIDITY_HOURS = 72;

    private final SecureRandom secureRandom = new SecureRandom();

    @ConfigProperty(name = "payment.reference.entity-code", defaultValue = DEFAULT_ENTITY)
    String defaultEntityCode;

    @ConfigProperty(name = "payment.reference.validity-hours", defaultValue = "72")
    int validityHours;

    /**
     * Issues a MultiCaixa payment reference as formatted text.
     *
     * @param request the reference request (merchantTransactionId, amount, optional referenceCode/entityCode)
     * @return reference as text (reference number, entity, amount, due date)
     */
    public PaymentReferenceResponse issueReference(PaymentReferenceRequest request) {
        if (request == null || request.getMerchantTransactionId() == null || request.getAmount() == null) {
            throw new IllegalArgumentException("merchantTransactionId and amount are required");
        }
        String referenceNumber = generateReferenceNumber(request.getReferenceCode());
        String entityCode = request.getEntityCode() != null && !request.getEntityCode().isBlank()
                ? request.getEntityCode()
                : defaultEntityCode;
        Instant dueDate = Instant.now().plus(validityHours, ChronoUnit.HOURS);

        String referenceText = formatReferenceText(referenceNumber, entityCode, request.getAmount(), dueDate);

        LOG.infof("Issued text reference: ref=%s entity=%s amount=%s order=%s",
                referenceNumber, entityCode, request.getAmount(), request.getMerchantTransactionId());

        return PaymentReferenceResponse.builder()
                .referenceNumber(referenceNumber)
                .entityCode(entityCode)
                .referenceText(referenceText)
                .amount(request.getAmount())
                .currency(CURRENCY_AOA)
                .dueDate(dueDate)
                .merchantTransactionId(request.getMerchantTransactionId())
                .build();
    }

    /**
     * Formats reference for MultiCaixa ATM display.
     */
    String formatReferenceText(String referenceNumber, String entityCode, BigDecimal amount, Instant dueDate) {
        return String.format(Locale.US,
                "REF: %s%nENTITY: %s%nAMOUNT: %,.2f AOA%nDUE: %tF %<tR",
                referenceNumber, entityCode, amount, dueDate);
    }

    /**
     * Generates a 9-digit reference number. Uses referenceCode as seed if provided for consistency.
     */
    String generateReferenceNumber(String referenceCode) {
        if (referenceCode != null && !referenceCode.isBlank() && referenceCode.matches("\\d{9}")) {
            return referenceCode;
        }
        int max = (int) Math.pow(10, REFERENCE_LENGTH);
        int min = (int) Math.pow(10, REFERENCE_LENGTH - 1);
        int value = min + secureRandom.nextInt(max - min);
        return String.valueOf(value);
    }
}
