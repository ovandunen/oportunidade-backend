package ao.co.oportunidade.webhook.entity;

import ao.co.oportunidade.webhook.WebhookEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for WebhookEvent domain and WebhookEventEntity.
 */
@Mapper(componentModel = "cdi")
public interface WebhookEventEntityMapper extends ao.co.oportunidade.entity.EntityMapper<WebhookEvent, WebhookEventEntity> {

    @Mapping(target = "processingStatus", source = "processingStatus")
    WebhookEventEntity mapToEntity(WebhookEvent domain);

    @Mapping(target = "processingStatus", source = "processingStatus")
    WebhookEvent mapToDomain(WebhookEventEntity entity);

    default String mapStatusToString(WebhookEvent.ProcessingStatus status) {
        return status != null ? status.name() : null;
    }

    default WebhookEvent.ProcessingStatus mapStringToStatus(String status) {
        return status != null ? WebhookEvent.ProcessingStatus.valueOf(status) : null;
    }
}
