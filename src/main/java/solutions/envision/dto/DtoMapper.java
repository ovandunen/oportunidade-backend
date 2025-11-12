package solutions.envision.dto;


public interface DtoMapper<DT,D> {

    DT mapToDto(D domain);
    D mapToDomain(DT dto);
}
