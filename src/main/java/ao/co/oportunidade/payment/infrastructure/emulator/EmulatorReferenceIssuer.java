package ao.co.oportunidade.payment.infrastructure.emulator;

import ao.co.oportunidade.payment.infrastructure.client.AppyPayApiClient;
import ao.co.oportunidade.payment.infrastructure.client.dto.AppyPayReferenceRequest;
import ao.co.oportunidade.payment.model.IssuedReference;
import ao.co.oportunidade.payment.model.ReferenceIssuanceCommand;
import ao.co.oportunidade.payment.service.ReferenceIssuancePort;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
@IfBuildProfile("dev")
public class EmulatorReferenceIssuer implements ReferenceIssuancePort {

    @Inject
    @RestClient
    AppyPayApiClient appyPayApiClient;

    @Inject
    @ConfigProperty(name = "payment.reference.entity-code", defaultValue = "00123")
    String entityCode;

    @Override
    public IssuedReference issue(ReferenceIssuanceCommand command) {
        String merchantTxId = command.getMerchantTransactionId().toString();
        String currency = command.getCurrency() != null ? command.getCurrency() : "AOA";

        var amountItem = new AppyPayReferenceRequest.AmountItem();
        amountItem.setAmount(command.getAmount());

        var refItem = new AppyPayReferenceRequest.ReferenceItem();
        refItem.setMerchantTransactionId(merchantTxId);
        refItem.setCurrency(currency);
        refItem.setAmounts(List.of(amountItem));

        var request = new AppyPayReferenceRequest();
        request.setPaymentMethod("REF");
        request.setReferences(List.of(refItem));

        var response = appyPayApiClient.createReferences(request);

        if (response.getReferences() == null || response.getReferences().isEmpty()) {
            throw new IllegalStateException("Emulator returned no references");
        }

        var first = response.getReferences().get(0);
        return IssuedReference.of(first.getReferenceNumber(), entityCode);
    }
}
