package ao.co.oportunidade.payment;

import ao.co.oportunidade.payment.model.IssuedReference;
import ao.co.oportunidade.payment.model.ReferenceIssuanceCommand;
import ao.co.oportunidade.payment.service.ReferenceIssuancePort;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


import org.eclipse.microprofile.rest.client.inject.RestClient;

import ao.co.oportunidade.payment.infrastructure.client.AppyPayApiClient;
import ao.co.oportunidade.payment.infrastructure.client.dto.AppyPayReferenceRequest;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
@UnlessBuildProfile("dev")  // Use emulator in dev; use this in prod/test
public class AppyPayReferenceIssuancePort implements ReferenceIssuancePort {

    @Inject
    @RestClient
    AppyPayApiClient appyPayApiClient;

    @Override
    public IssuedReference issue(ReferenceIssuanceCommand command) {
        String merchantTxId = command.getMerchantTransactionId().toString();
        String currency = command.getCurrency() != null ? command.getCurrency() : "AOA";
        String expiresAtIso = command.getExpiresAt() != null
                ? command.getExpiresAt().toString()
                : Instant.now().plusSeconds(86400).toString();

        var amountItem = new AppyPayReferenceRequest.AmountItem();
        amountItem.setAmount(command.getAmount());
        amountItem.setDescriptionLine1("Payment");
        amountItem.setDescriptionLine2(merchantTxId);

        var refItem = new AppyPayReferenceRequest.ReferenceItem();
        refItem.setMerchantTransactionId(merchantTxId);
        refItem.setCurrency(currency);
        refItem.setAmounts(List.of(amountItem));
        refItem.setExpirationDate(expiresAtIso);

        var request = new AppyPayReferenceRequest();
        request.setPaymentMethod("REF");
        request.setCreatedBy("recruiting-app");
        request.setReferences(List.of(refItem));

        var response = appyPayApiClient.createReferences(request);

        if (response.getReferences() == null || response.getReferences().isEmpty()) {
            throw new IllegalStateException("AppyPay returned no references");
        }

        var first = response.getReferences().getFirst();
        // Production AppyPay response may include entity; use configured default if not
        String entityCode = "00123";
        return IssuedReference.of(first.getReferenceNumber(), entityCode);
    }
}
