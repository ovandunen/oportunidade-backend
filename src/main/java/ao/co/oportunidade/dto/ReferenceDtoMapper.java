package ao.co.oportunidade.dto;

import ao.co.oportunidade.Reference;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface ReferenceDtoMapper extends DtoMapper<ReferenceDTO, Reference> {
}
