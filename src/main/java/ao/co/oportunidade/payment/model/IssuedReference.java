package ao.co.oportunidade.payment.model;


import lombok.Getter;
import solutions.envision.model.DomainValue;


@Getter
public class IssuedReference implements DomainValue {

        private final String referenceNumber;
        private final String entityCode;

        private IssuedReference(final String referenceNumber, final String entityCode) {
            if (referenceNumber == null || referenceNumber.isBlank())
                throw new IllegalArgumentException(
                        "referenceNumber must be provided by the payment network");
            if (entityCode == null || entityCode.isBlank())
                throw new IllegalArgumentException(
                        "entityCode must be provided by the payment network");
            this.referenceNumber = referenceNumber;
            this.entityCode = entityCode;
        }

        public static IssuedReference of(String referenceNumber, String entityCode) {
            return new IssuedReference(referenceNumber, entityCode);
        }

}
