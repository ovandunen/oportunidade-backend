package ao.co.oportunidade.webhook.entity;

import ao.co.oportunidade.Reference;
import ao.co.oportunidade.entity.EntityMapper;
import ao.co.oportunidade.entity.ReferenceEntity;
import org.mapstruct.Mapper;

@Mapper
public interface ReferenceEntityMapper extends EntityMapper<Reference, ReferenceEntity> {
}
