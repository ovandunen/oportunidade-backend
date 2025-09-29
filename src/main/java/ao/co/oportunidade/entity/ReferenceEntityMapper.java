package ao.co.oportunidade.entity;

import ao.co.oportunidade.Reference;
import org.mapstruct.Mapper;

@Mapper(componentModel ="cdi")
public interface ReferenceEntityMapper extends EntityMapper<Reference, ReferenceEntity> {
}
