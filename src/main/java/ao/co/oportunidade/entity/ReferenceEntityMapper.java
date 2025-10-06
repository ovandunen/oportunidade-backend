package ao.co.oportunidade.entity;

import ao.co.oportunidade.Reference;
import ao.co.oportunidade.valueobject.AmountValueObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;

@Mapper(componentModel ="cdi")
@MapperConfig( uses = AmountValueObjectMapper.class)
public interface ReferenceEntityMapper extends EntityMapper<Reference, ReferenceEntity> {
}
