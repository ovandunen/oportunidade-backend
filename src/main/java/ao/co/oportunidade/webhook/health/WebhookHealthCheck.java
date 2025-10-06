package ao.co.oportunidade.webhook.health;

import ao.co.oportunidade.webhook.WebhookEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health check for webhook service.
 * Verifies that the webhook processing system is operational.
 * Refactored to use DDD repository.
 */
@Readiness
@ApplicationScoped
public class WebhookHealthCheck implements HealthCheck {

    @Inject
    WebhookEventRepository webhookEventRepository;

    @Override
    public HealthCheckResponse call() {
        try {
            // Simple check: can we access the database?
            long count = webhookEventRepository.count();
            
            return HealthCheckResponse.up("webhook-service")
                    .withData("total_events", count)
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.down("webhook-service")
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
