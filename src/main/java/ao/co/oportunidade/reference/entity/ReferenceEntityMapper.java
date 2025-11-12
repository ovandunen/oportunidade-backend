package ao.co.oportunidade.reference.entity;

import ao.co.oportunidade.reference.model.Reference;
import ao.co.oportunidade.order.model.AmountValueObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import solutions.envision.entity.EntityMapper;

@Mapper(componentModel ="cdi")
@MapperConfig( uses = AmountValueObjectMapper.class)
public interface ReferenceEntityMapper extends EntityMapper<Reference, ReferenceEntity> {
}
