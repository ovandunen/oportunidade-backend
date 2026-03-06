package ao.co.oportunidade.payment.service;

import ao.co.oportunidade.payment.model.IssuedReference;
import ao.co.oportunidade.payment.model.ReferenceIssuanceCommand;

public interface ReferenceIssuancePort {

        IssuedReference issue(ReferenceIssuanceCommand command);
}
