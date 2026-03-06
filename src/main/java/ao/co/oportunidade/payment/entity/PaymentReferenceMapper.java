package ao.co.oportunidade.payment.entity;

import ao.co.oportunidade.payment.model.PaymentReference;
import org.mapstruct.Mapper;
import solutions.envision.entity.EntityMapper;


@Mapper(componentModel = "cdi")
public interface PaymentReferenceMapper extends EntityMapper<PaymentReference, PaymentReferenceEntity> {
}
