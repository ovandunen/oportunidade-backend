package ao.co.oportunidade.reference.dto;

import ao.co.oportunidade.reference.model.Reference;
import org.mapstruct.Mapper;
import solutions.envision.dto.DtoMapper;

@Mapper(componentModel = "cdi")
public interface ReferenceDtoMapper extends DtoMapper<ReferenceDTO, Reference> {
}
