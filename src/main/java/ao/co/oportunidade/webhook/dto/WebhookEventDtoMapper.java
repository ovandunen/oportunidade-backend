package ao.co.oportunidade.webhook.dto;

import ao.co.oportunidade.webhook.WebhookEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import solutions.envision.dto.DtoMapper;

/**
 * MapStruct mapper for WebhookEventDTO and WebhookEvent domain.
 */
@Mapper(componentModel = "cdi")
public interface WebhookEventDtoMapper extends DtoMapper<WebhookEventDTO, WebhookEvent> {

    @Mapping(target = "processingStatus", source = "processingStatus")
    WebhookEventDTO mapToDto(WebhookEvent domain);

    @Mapping(target = "processingStatus", source = "processingStatus")
    WebhookEvent mapToDomain(WebhookEventDTO dto);

    default String mapStatusToString(WebhookEvent.ProcessingStatus status) {
        return status != null ? status.name() : null;
    }

    default WebhookEvent.ProcessingStatus mapStringToStatus(String status) {
        return status != null ? WebhookEvent.ProcessingStatus.valueOf(status) : null;
    }
}
