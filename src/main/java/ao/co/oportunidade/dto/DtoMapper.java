package ao.co.oportunidade.dto;


public interface DtoMapper<DT,D> {

    DT mapDomainToDto(D domain);
    D mapDtoToDomain(DT dto);
}
